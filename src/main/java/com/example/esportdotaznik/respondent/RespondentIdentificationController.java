package com.example.esportdotaznik.respondent;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.Map;

@Controller
public class RespondentIdentificationController {

    private final RespondentIdentificationService respondentIdentificationService;

    public RespondentIdentificationController(RespondentIdentificationService respondentIdentificationService) {
        this.respondentIdentificationService = respondentIdentificationService;
    }

    @GetMapping("/identification")
    public String showForm(Model model) {
        populateFormModel(model, new RespondentIdentificationForm(), Map.of());
        return "respondent-identification-form";
    }

    @PostMapping("/identification")
    public String createIdentification(@ModelAttribute RespondentIdentificationForm form, Model model) {
        Map<String, String> fieldErrors = respondentIdentificationService.validate(form);
        if (!fieldErrors.isEmpty()) {
            populateFormModel(model, form, fieldErrors);
            return "respondent-identification-form";
        }

        RespondentIdentificationResult result = respondentIdentificationService.createIdentification(form);
        populateFormModel(model, form, Map.of());
        model.addAttribute("result", result);
        return "respondent-identification-form";
    }

    private void populateFormModel(Model model, RespondentIdentificationForm form, Map<String, String> fieldErrors) {
        model.addAttribute("form", form);
        model.addAttribute("fieldErrors", fieldErrors);
        model.addAttribute("schoolOptions", respondentIdentificationService.schoolOptions());
        model.addAttribute("serviceOptions", respondentIdentificationService.serviceOptions());
        model.addAttribute("sexOptions", respondentIdentificationService.sexOptions());
        model.addAttribute("gamerLevelOptions", respondentIdentificationService.gamerLevelOptions());
    }
}
