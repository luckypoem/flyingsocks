package com.lzf.flyingsocks.client.gui.swt;

import com.lzf.flyingsocks.AbstractModule;
import com.lzf.flyingsocks.Config;
import com.lzf.flyingsocks.client.ClientOperator;
import com.lzf.flyingsocks.client.gui.ResourceManager;
import com.lzf.flyingsocks.util.BaseUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import static com.lzf.flyingsocks.client.proxy.ProxyServerConfig.*;
import static com.lzf.flyingsocks.client.gui.swt.Utils.*;

/**
 * @create 2019.8.17 23:05
 * @description 服务器设置界面
 */
final class ServerSettingModule extends AbstractModule<SWTViewComponent> {

    private static final Logger log = LoggerFactory.getLogger("ServerSettingUI");

    private final Display display;

    private final Shell shell;

    private final ClientOperator operator;

    private final ServerList serverList;

    private final ServerSettingForm serverSettingForm;

    ServerSettingModule(SWTViewComponent component, Display display) {
        super(Objects.requireNonNull(component));
        this.display = display;
        this.operator = component.getParentComponent();

        Image icon;
        try (InputStream is = ResourceManager.openIconImageStream()) {
            icon = new Image(display, is);
        } catch (IOException e) {
            throw new Error(e);
        }

        this.shell = createShell(display, "服务器设置", icon, 850, 370);
        this.serverList = new ServerList();
        this.serverSettingForm = new ServerSettingForm();

        setVisiable(false);
        adaptDPI(shell);
    }


    private final class ServerList {
        private final List serverList;
        private final Map<Integer, Node> serverMap = new TreeMap<>();
        int select = 0;

        ServerList() {
            serverList = new List(shell, SWT.SINGLE | SWT.V_SCROLL | SWT.BORDER);
            serverList.setBounds(10, 10, 250, 305);
            serverList.setToolTipText("服务器列表");
            serverList.add("点击此处进行添加", 0);
            serverList.select(0);

            addListSelectionListener(serverList, e -> {
                int idx = serverList.getSelectionIndex();
                select = idx;
                Node n = serverMap.get(idx);
                if(n == null) {
                    serverSettingForm.cleanForm();
                    return;
                }
                serverSettingForm.setHostText(n.getHost());
                serverSettingForm.setPortText(n.getPort());
                serverSettingForm.setCertPortText(n.getCertPort());
                serverSettingForm.setEncrypt(n.getEncryptType());
                serverSettingForm.setAuth(n.getAuthType());

                if(n.getAuthType() == AuthType.USER) {
                    serverSettingForm.setUser(n.getAuthArgument("user"));
                    serverSettingForm.setPass(n.getAuthArgument("pass"));
                } else {
                    serverSettingForm.setPass(n.getAuthArgument("password"));
                }

            });

            flush(false);
            operator.registerProxyServerConfigListener(Config.UPDATE_EVENT, () -> flush(true), false);
        }

        private void flush(boolean clean) {
            if(clean) {
                serverList.remove(1, serverList.getItemCount() - 1);
                serverMap.clear();
            }

            Node[] nodes = operator.getServerNodes();
            int i = 1;
            for (Node node : nodes) {
                serverMap.put(i, node);
                serverList.add(node.getHost() + ":" + node.getPort(), i++);
            }
        }

        private Node selectNode() {
            return select != -1 && select != 0 ? serverMap.get(select) : null;
        }

        boolean contains(String host, int port) {
            for (Node n : serverMap.values()) {
                if(n.getHost().equals(host) && n.getPort() == port) {
                    return true;
                }
            }
            return false;
        }
    }

    private final class ServerSettingForm {
        private final Text host;
        private final Text port;
        private final Text certPort;
        private final Combo encrypt;
        private final Combo auth;
        private final Text user;
        private final Text pass;

        ServerSettingForm() {
            Text host = new Text(shell, SWT.BORDER);
            Text port = new Text(shell, SWT.BORDER);
            Text certPort = new Text(shell, SWT.BORDER);
            Combo encrypt = new Combo(shell, SWT.DROP_DOWN | SWT.READ_ONLY);
            Combo auth = new Combo(shell, SWT.DROP_DOWN | SWT.READ_ONLY);

            Text user = new Text(shell, SWT.BORDER);
            Text pass = new Text(shell, SWT.BORDER | SWT.PASSWORD);

            createLabel(shell, "地址", 270, 10, 70, 30, SWT.CENTER);
            createLabel(shell, "地址", 270, 10, 70, 30, SWT.CENTER);
            createLabel(shell, "端口", 270, 50, 70, 30, SWT.CENTER);
            createLabel(shell, "证书端口", 550, 50, 80, 30, SWT.CENTER);
            createLabel(shell, "加密方式", 270, 90, 80, 30, SWT.CENTER);
            createLabel(shell, "认证方式", 270, 130, 80, 30, SWT.CENTER);
            createLabel(shell, "用户名", 270, 170, 70, 30, SWT.CENTER);
            createLabel(shell, "密码", 270, 210, 70, 30, SWT.CENTER);

            host.setBounds(360, 10, 460, 30);
            port.setBounds(360, 50, 180, 30); certPort.setBounds(640, 50, 180, 30);
            encrypt.setBounds(360, 90, 460, 30);
            auth.setBounds(360, 130, 460, 30);
            user.setBounds(360,170, 460, 30);
            pass.setBounds(360, 210, 460, 30);

            encrypt.add("无加密", 0);
            encrypt.add("TLS/SSL", 1);
            auth.add("普通认证", 0);
            auth.add("用户认证", 1);

            encrypt.select(0);
            auth.select(1);
            user.setEditable(false);

            addComboSelectionListener(auth, e -> {
                int idx = auth.getSelectionIndex();
                if(idx == 0) {
                    user.setText("");
                    user.setEditable(false);
                } else {
                    user.setEditable(true);
                }
            });

            addComboSelectionListener(encrypt, e -> {
                int idx = encrypt.getSelectionIndex();
                if(idx == 0) {
                    certPort.setText("");
                    certPort.setEditable(false);
                } else {
                    certPort.setEditable(true);
                }
            });

            Button save = new Button(shell, SWT.PUSH);
            save.setText("保存");
            save.setBounds(270, 250, 270, 60);

            try (InputStream is = ResourceManager.openSaveIconImageStream()) {
                save.setImage(new Image(display, is));
            } catch (IOException e) {
                log.warn("Can not read save-icon image.", e);
            }

            addButtonSelectionListener(save, e -> {
                String _host = ServerSettingForm.this.host.getText();
                if(!BaseUtils.isIPv4Address(_host) && !BaseUtils.isHostName(_host)) {
                    showMessageBox(shell, "错误", "服务器主机名格式有误:需要为IPv4地址或是合法主机名/域名", SWT.ICON_ERROR | SWT.OK);
                    return;
                }

                String _ports = ServerSettingForm.this.port.getText();
                int _port;
                if(!BaseUtils.isPortString(_ports)) {
                    showMessageBox(shell, "错误", "端口号有误,必须为1~65535之间的数字", SWT.ICON_ERROR | SWT.OK);
                    return;
                }

                _port = Integer.parseInt(_ports);
                int en = encrypt.getSelectionIndex();
                String cports = ServerSettingForm.this.certPort.getText();
                int cport = 0;
                if(en == 1) {
                    if(!BaseUtils.isPortString(cports)) {
                        showMessageBox(shell, "错误", "证书端口号有误,必须为1~65535之间的数字", SWT.ICON_ERROR | SWT.OK);
                        return;
                    }
                    cport = Integer.parseInt(cports);
                }

                int au = auth.getSelectionIndex();
                String _user = null, pwd;
                pwd = ServerSettingForm.this.pass.getText();
                if(au == 1) {
                    _user = ServerSettingForm.this.user.getText();
                }

                Node n = serverList.selectNode();
                if(n == null) {
                    n = new Node();
                }
                n.setHost(_host);
                n.setPort(_port);
                n.setCertPort(cport);
                n.setEncryptType(en == 1 ? EncryptType.SSL : EncryptType.NONE);
                n.setAuthType(au == 1 ? AuthType.USER : AuthType.SIMPLE);
                if(au == 0) {
                    n.setAuthArgument(Collections.singletonMap("password", pwd));
                } else {
                    Map<String, String> map = new HashMap<>(2, 1);
                    map.put("user", _user);
                    map.put("pass", pwd);
                    n.setAuthArgument(map);
                }

                if(serverList.select == -1 || serverList.select == 0) {
                    if(serverList.contains(_host, _port)) {
                        showMessageBox(shell, "提示", "已经包含服务器 " + _host + ":" + _port + " 的配置", SWT.ICON_ERROR | SWT.OK);
                        return;
                    }
                    operator.addServerConfig(n);
                    showMessageBox(shell, "成功", "成功添加服务器配置 " + _host + ":" + _port, SWT.ICON_INFORMATION | SWT.OK);
                } else {
                    operator.updateServerConfig(n);
                    showMessageBox(shell, "成功", "成功修改服务器配置 " + _host + ":" + _port, SWT.ICON_INFORMATION | SWT.OK);
                }
            });

            Button delete = new Button(shell, SWT.PUSH);
            delete.setText("删除");
            delete.setBounds(550, 250, 270, 60);

            try(InputStream is = ResourceManager.openDeleteIconImageStream()) {
                delete.setImage(new Image(display, is));
            } catch (IOException e) {
                log.warn("Can not read delete-icon image.", e);
            }

            addButtonSelectionListener(delete, e -> {
                Node n = serverList.selectNode();
                if(n == null) {
                    showMessageBox(shell, "提示", "请选择需要删除的服务器配置", SWT.ICON_INFORMATION | SWT.OK);
                    return;
                }

                if(n.isUse()) {
                    operator.setProxyServerUsing(n, false);
                }

                operator.removeServer(n);
            });

            this.encrypt = encrypt;
            this.auth = auth;
            this.host = host;
            this.port = port;
            this.certPort = certPort;
            this.user = user;
            this.pass = pass;
        }

        void setHostText(String text) {
            host.setText(text);
        }

        void setPortText(int num) {
            if(num < 0) {
                port.setText("");
            } else {
                port.setText(String.valueOf(num));
            }
        }

        void setCertPortText(int num) {
            if(num < 0) {
                certPort.setText("");
            } else {
                certPort.setText(String.valueOf(num));
            }
        }

        void setEncrypt(EncryptType type) {
            switch (type) {
                case NONE: encrypt.select(0); break;
                case SSL: encrypt.select(1); break;
            }
        }

        void setAuth(AuthType type) {
            switch (type) {
                case SIMPLE: {
                    auth.select(0);
                    user.setEditable(false);
                } break;

                case USER: {
                    auth.select(1);
                    user.setEditable(true);
                } break;
            }
        }

        void setUser(String text) {
            user.setText(text);
        }

        void setPass(String text) {
            pass.setText(text);
        }

        void cleanForm() {
            setHostText("");
            setPortText(-1);
            setCertPortText(-1);
            setEncrypt(EncryptType.SSL);
            setAuth(AuthType.SIMPLE);
            setUser("");
            setPass("");
        }
    }


    void setVisiable(boolean visiable) {
        shell.setVisible(visiable);
    }


}
