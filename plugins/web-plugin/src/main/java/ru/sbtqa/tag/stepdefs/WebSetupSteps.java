package ru.sbtqa.tag.stepdefs;

import ru.sbtqa.tag.pagefactory.PageManager;
import ru.sbtqa.tag.pagefactory.environment.Environment;
import ru.sbtqa.tag.pagefactory.tasks.TaskHandler;
import ru.sbtqa.tag.pagefactory.web.drivers.WebDriverService;
import ru.sbtqa.tag.pagefactory.web.tasks.KillAlertTask;

public class WebSetupSteps {

    private WebSetupSteps() {}

    public static synchronized void initWeb() {
        PageManager.cachePages();

        if (isNewDriverNeeded()) {
            Environment.setDriverService(new WebDriverService());
        }
    }

    private static boolean isNewDriverNeeded() {
        return Environment.isDriverEmpty();
    }

    public static synchronized void disposeWeb() {
        TaskHandler.addTask(new KillAlertTask());
    }
}