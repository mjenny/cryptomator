package org.cryptomator.ui.traymenu;

import dorkbox.systemTray.SystemTray;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.ui.fxapp.FxApplication;
import org.cryptomator.ui.launcher.AppLifecycleListener;
import org.cryptomator.ui.launcher.FxApplicationStarter;
import org.cryptomator.ui.preferences.SelectedPreferencesTab;

import javax.inject.Inject;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.collections.ObservableList;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.EventObject;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.function.Consumer;

@TrayMenuScoped
class TrayMenuController {

	private final ResourceBundle resourceBundle;
	private final AppLifecycleListener appLifecycle;
	private final FxApplicationStarter fxApplicationStarter;
	private final ObservableList<Vault> vaults;
	private final JMenu menu;

	@Inject
	TrayMenuController(ResourceBundle resourceBundle, AppLifecycleListener appLifecycle, FxApplicationStarter fxApplicationStarter, ObservableList<Vault> vaults) {
		this.resourceBundle = resourceBundle;
		this.appLifecycle = appLifecycle;
		this.fxApplicationStarter = fxApplicationStarter;
		this.vaults = vaults;
		this.menu = new JMenu();
	}

	public JMenu getMenu() {
		return menu;
	}

	public void initTrayMenu() {
		vaults.addListener(this::vaultListChanged);
		rebuildMenu();
	}

	private void vaultListChanged(@SuppressWarnings("unused") Observable observable) {
		assert Platform.isFxApplicationThread();
		rebuildMenu();
	}

	private void rebuildMenu() {
		menu.removeAll();

		JMenuItem showMainWindowItem = new JMenuItem(resourceBundle.getString("traymenu.showMainWindow"));
		showMainWindowItem.addActionListener(this::showMainWindow);
		menu.add(showMainWindowItem);

		JMenuItem showPreferencesItem = new JMenuItem(resourceBundle.getString("traymenu.showPreferencesWindow"));
		showPreferencesItem.addActionListener(this::showPreferencesWindow);
		menu.add(showPreferencesItem);

		if (vaults.size() > 0) {
			menu.addSeparator();
			for (Vault v : vaults) {
				JMenuItem submenu = buildSubmenu(v);
				menu.add(submenu);
			}
			menu.addSeparator();
		}

		JMenuItem lockAllItem = new JMenuItem(resourceBundle.getString("traymenu.lockAllVaults"));
		lockAllItem.addActionListener(this::lockAllVaults);
		lockAllItem.setEnabled(!vaults.filtered(Vault::isUnlocked).isEmpty());
		menu.add(lockAllItem);

		JMenuItem quitApplicationItem = new JMenuItem(resourceBundle.getString("traymenu.quitApplication"));
		quitApplicationItem.addActionListener(this::quitApplication);
		menu.add(quitApplicationItem);

		SystemTray systemTray = SystemTray.get("Cryptomator");
		systemTray.getMenu().getEntries().stream().forEach(e -> systemTray.getMenu().remove(e));
		systemTray.setMenu(this.menu);
	}

	private JMenuItem buildSubmenu(Vault vault) {
		JMenu submenu = new JMenu(vault.getDisplayName());

		if (vault.isLocked()) {
			JMenuItem unlockItem = new JMenuItem(resourceBundle.getString("traymenu.vault.unlock"));
			unlockItem.addActionListener(createActionListenerForVault(vault, this::unlockVault));
			submenu.add(unlockItem);
		} else if (vault.isUnlocked()) {
			JMenuItem lockItem = new JMenuItem(resourceBundle.getString("traymenu.vault.lock"));
			lockItem.addActionListener(createActionListenerForVault(vault, this::lockVault));
			submenu.add(lockItem);

			JMenuItem revealItem = new JMenuItem(resourceBundle.getString("traymenu.vault.reveal"));
			revealItem.addActionListener(createActionListenerForVault(vault, this::revealVault));
			submenu.add(revealItem);
		}

		return submenu;
	}

	private ActionListener createActionListenerForVault(Vault vault, Consumer<Vault> consumer) {
		return actionEvent -> consumer.accept(vault);
	}

	private void quitApplication(EventObject actionEvent) {
		appLifecycle.quit();
	}

	private void unlockVault(Vault vault) {
		showMainAppAndThen(app -> app.startUnlockWorkflow(vault, Optional.empty()));
	}

	private void lockVault(Vault vault) {
		showMainAppAndThen(app -> app.startLockWorkflow(vault, Optional.empty()));
	}

	private void lockAllVaults(ActionEvent actionEvent) {
		showMainAppAndThen(app -> app.getVaultService().lockAll(vaults.filtered(Vault::isUnlocked), false));
	}

	private void revealVault(Vault vault) {
		showMainAppAndThen(app -> app.getVaultService().reveal(vault));
	}

	void showMainWindow(@SuppressWarnings("unused") ActionEvent actionEvent) {
		showMainAppAndThen(app -> app.showMainWindow());
	}

	private void showPreferencesWindow(@SuppressWarnings("unused") EventObject actionEvent) {
		showMainAppAndThen(app -> app.showPreferencesWindow(SelectedPreferencesTab.ANY));
	}

	private void showMainAppAndThen(Consumer<FxApplication> action) {
		fxApplicationStarter.get().thenAccept(action);
	}

}
