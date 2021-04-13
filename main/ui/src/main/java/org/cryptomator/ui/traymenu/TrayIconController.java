package org.cryptomator.ui.traymenu;

import com.google.common.base.Preconditions;
import org.apache.commons.lang3.SystemUtils;
import org.cryptomator.integrations.uiappearance.Theme;
import org.cryptomator.integrations.uiappearance.UiAppearanceException;
import org.cryptomator.integrations.uiappearance.UiAppearanceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import dorkbox.systemTray.SystemTray;

import java.awt.Image;
import java.util.Optional;

@TrayMenuScoped
public class TrayIconController {

	private static final Logger LOG = LoggerFactory.getLogger(TrayIconController.class);
	private static final String TOOLTIP = "Cryptomator";

	private final TrayImageFactory imageFactory;
	private final Optional<UiAppearanceProvider> appearanceProvider;
	private final TrayMenuController trayMenuController;
	private final Image icon;
	private volatile boolean initialized;

	@Inject
	TrayIconController(TrayImageFactory imageFactory, TrayMenuController trayMenuController, Optional<UiAppearanceProvider> appearanceProvider) {
		this.trayMenuController = trayMenuController;
		this.imageFactory = imageFactory;
		this.appearanceProvider = appearanceProvider;
		this.icon = imageFactory.loadImage();
	}

	public synchronized void initializeTrayIcon() throws IllegalStateException {
		Preconditions.checkState(!initialized);

		appearanceProvider.ifPresent(appearanceProvider -> {
			try {
				appearanceProvider.addListener(this::systemInterfaceThemeChanged);
			} catch (UiAppearanceException e) {
				LOG.error("Failed to enable automatic tray icon theme switching.");
			}
		});

		SystemTray systemTray = SystemTray.get("Cryptomator");

		if (systemTray != null) {
			systemTray.setEnabled(false);
			systemTray.installShutdownHook();
			systemTray.setImage(icon);
			systemTray.setTooltip(TOOLTIP);
			systemTray.setEnabled(true);
		} else {
			LOG.error("Error adding tray icon");
		}

		trayMenuController.initTrayMenu();

		this.initialized = true;
	}

	private void systemInterfaceThemeChanged(Theme theme) {
		SystemTray.get("Cryptomator").setImage(imageFactory.loadImage()); // TODO refactor "theme" is re-queried in loadImage()
	}

	public boolean isInitialized() {
		return initialized;
	}
}
