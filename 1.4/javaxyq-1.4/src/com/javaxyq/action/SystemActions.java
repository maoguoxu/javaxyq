package com.javaxyq.action;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import com.javaxyq.core.DialogFactory;
import com.javaxyq.core.GameMain;
import com.javaxyq.event.ActionEvent;
import com.javaxyq.util.BrowserLauncher;
import com.javaxyq.widget.Player;
import com.javaxyq.ui.*;

/**
 * 系统事件处理代码
 */
public class SystemActions extends BaseAction {

	private int dlgIdIndex = "com.javaxyq.action.dialog.".length();
    public void doAction(ActionEvent e) {
        String cmd = e.getCommand();
        Object source = e.getSource();
        System.out.println("action: "+cmd);
        if (cmd.startsWith("com.javaxyq.action.dialog.")) {
            UIHelper.showHideDialog(cmd.substring(dlgIdIndex));
        } else if (cmd.startsWith("com.javaxyq.action.transport")) {
            String sceneId = e.getArgumentAsString(0);
            int x = e.getArgumentAsInt(1);
            int y = e.getArgumentAsInt(2);
            GameMain.fadeToMap(sceneId, x, y);
        } else {

        }
    }

}
