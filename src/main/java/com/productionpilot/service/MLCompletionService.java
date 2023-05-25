/*
 * Copyright (c) 2022-2023 Felix Kirchmann.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */

package com.productionpilot.service;

import com.productionpilot.db.timescale.entities.Machine;
import com.productionpilot.db.timescale.entities.Parameter;
import com.productionpilot.db.timescale.service.ParameterService;
import com.productionpilot.opc.OpcNode;
import com.productionpilot.util.DebugPerfTimer;
import com.theokanning.openai.OpenAiService;
import com.theokanning.openai.completion.CompletionRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import retrofit2.HttpException;

import javax.annotation.Nonnull;
import javax.annotation.PostConstruct;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class MLCompletionService {
    @Value("${com.productionpilot.openai.model}")
    private String model;
    @Value("${com.productionpilot.openai.api-token:#{null}}")
    private String apiToken;
    @Value("${com.productionpilot.openai.max-prompt-length}")
    private int maxPromptLength;

    private static final int selectNewestParameters = 10;

    private static final int choiceCount = 5;

    private final ParameterService parameterService;

    private final ParameterRecordingService parameterRecordingService;

    private OpenAiService service;

    @PostConstruct
    private void init() {
        service = new OpenAiService(apiToken);
    }

    public List<String> getCompletion(@Nonnull OpcNode node, @Nonnull String startsWith) {
        if(model == null || apiToken == null) {
            return Collections.emptyList();
        }
        var prompt = getPromptPrefix() + "Tag \"" + node.getPath() + "\" is named \"" + startsWith;
        log.debug("Using model {}. Prompt ({} chars):\n{}", model, prompt.length(), prompt);
        CompletionRequest completionRequest = CompletionRequest.builder()
                .model(model)
                .prompt(prompt)
                .stop(List.of("\""))
                .n(choiceCount)
                .build();
        DebugPerfTimer timer = DebugPerfTimer.start("MLCompletionService.getCompletion");
        List<String> result = List.of("");
        try {
            result = service.createCompletion(completionRequest).getChoices().stream().map(c -> startsWith + c.getText())
                    .distinct().sorted(Comparator.comparingInt(String::length)).toList();
        } catch (HttpException e) {
            if(e.getMessage().contains("429")) {
                log.warn("OpenAI API rate limit exceeded. Please wait a few minutes and try again.");
            } else {
                log.error("OpenAI API error", e);
            }
        }
        timer.endAndPrint(log);
        return result;
    }

    private String getPromptPrefix() {
        var parameterNodes = parameterRecordingService.listSubscribedItems().entrySet()
                .stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getNode()));
        var parametersNewestFirst = new ArrayList<>(parameterNodes.keySet().stream()
                .sorted(Comparator.comparing(Parameter::getId).reversed()).toList());
        StringBuilder prompt = new StringBuilder(maxPromptLength);
        prompt.append("This list maps Tags to descriptive names." +
                " The names are concise, descriptive and easy to understand.\n");
        for(int i = 0; i < Math.min(parametersNewestFirst.size(), selectNewestParameters); i++) {
            var entry = parametersNewestFirst.remove(0);
            if(!addPromptLineFromParameter(prompt, entry, parameterNodes.get(entry))) {
                return prompt.toString();
            }
        }
        var machineParameters = new HashMap<Machine, List<Parameter>>();
        for(Map.Entry<Machine, List<Parameter>> entry : parametersNewestFirst.stream()
                .collect(Collectors.groupingBy(Parameter::getMachine)).entrySet()) {
            // duplicate the list to ensure that it is mutable
            machineParameters.put(entry.getKey(), new ArrayList<>(entry.getValue()));
        }
        var mapIterator = machineParameters.entrySet().iterator();
        while(mapIterator.hasNext()) {
            var entry = mapIterator.next();
            if(entry.getValue().isEmpty()) {
                mapIterator.remove();
            } else {
                var parameter = entry.getValue().remove(0);
                if(!addPromptLineFromParameter(prompt, parameter, parameterNodes.get(parameter))) {
                    return prompt.toString();
                }
            }
            if(!mapIterator.hasNext()) {
                mapIterator = machineParameters.entrySet().iterator();
            }
        }
        return prompt.toString();
    }

    private boolean addPromptLineFromParameter(StringBuilder sb, Parameter p, OpcNode node) {
        var line = "Tag \"" + node.getPath() + "\" is named \"" + p.getName() + "\".\n";
        if(sb.length() + line.length() > maxPromptLength) {
            return false;
        } else {
            sb.append(line);
            return true;
        }
    }
}
