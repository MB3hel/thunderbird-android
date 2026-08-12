package com.fsck.k9.preferences.upgrader;


import java.util.Map;

import com.fsck.k9.preferences.SettingsUpgrader;


/**
 * Upgrades the settings from version 111 to 112
 *
 * <p>
 * Add new keepUnreadNotifications setting
 * </p>
 */
public class GeneralSettingsUpgraderTo112 implements SettingsUpgrader {

    @Override
    public void upgrade(Map<String, Object> settings) {
        settings.put("keepUnreadNotifications", false);
    }
}
