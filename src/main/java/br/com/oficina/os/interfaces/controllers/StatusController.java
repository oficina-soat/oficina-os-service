package br.com.oficina.os.interfaces.controllers;

import br.com.oficina.os.interfaces.presenters.view_model.StatusViewModel;
import java.util.concurrent.CompletableFuture;

public class StatusController {
    private final String applicationName;
    private final String environment;

    public StatusController(String applicationName, String environment) {
        this.applicationName = applicationName;
        this.environment = environment;
    }

    public CompletableFuture<StatusViewModel> status() {
        return CompletableFuture.completedFuture(new StatusViewModel(applicationName, environment, "UP"));
    }
}
