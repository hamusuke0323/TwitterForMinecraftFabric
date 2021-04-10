package com.hamusuke.twitter4mc.gui.screen;

import com.hamusuke.twitter4mc.Token;
import com.hamusuke.twitter4mc.TwitterForMC;
import com.hamusuke.twitter4mc.gui.toasts.TwitterNotificationToast;
import com.hamusuke.twitter4mc.gui.widget.MaskableTextFieldWidget;
import com.hamusuke.twitter4mc.utils.TwitterUtil;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CheckboxWidget;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.client.toast.SystemToast;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Util;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import twitter4j.Twitter;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;

import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.net.URI;
import java.util.function.Consumer;

@Environment(EnvType.CLIENT)
public class TwitterLoginScreen extends ParentalScreen {
    private static final Logger LOGGER = LogManager.getLogger();

    public TwitterLoginScreen(Screen parent) {
        super(new TranslatableText("twitter.login"), parent);
    }

    protected void init() {
        super.init();
        int i = this.width / 2;
        int j = this.width / 4;
        int k = this.width / 3;

        boolean flag = TwitterForMC.consumer == null;
        TwitterForMC.consumer = TwitterForMC.consumer != null ? TwitterForMC.consumer : new MaskableTextFieldWidget(this.font, j, 20, i, 20, I18n.translate("tw.consumer.key"), '●', 1000);
        TwitterForMC.consumer.x = j;
        TwitterForMC.consumer.setWidth(i);
        TwitterForMC.consumer.setMessage(I18n.translate("tw.consumer.key"));
        this.addButton(TwitterForMC.consumer);

        TwitterForMC.consumerS = TwitterForMC.consumerS != null ? TwitterForMC.consumerS : new MaskableTextFieldWidget(this.font, j, 60, i, 20, I18n.translate("tw.consumer.secret"), '●', 1000);
        TwitterForMC.consumerS.x = j;
        TwitterForMC.consumerS.setWidth(i);
        TwitterForMC.consumerS.setMessage(I18n.translate("tw.consumer.secret"));
        this.addButton(TwitterForMC.consumerS);

        TwitterForMC.access = TwitterForMC.access != null ? TwitterForMC.access : new MaskableTextFieldWidget(this.font, j, 100, i, 20, I18n.translate("tw.access.token"), '●', 1000);
        TwitterForMC.access.x = j;
        TwitterForMC.access.setWidth(i);
        TwitterForMC.access.setMessage(I18n.translate("tw.access.token"));
        this.addButton(TwitterForMC.access);

        TwitterForMC.accessS = TwitterForMC.accessS != null ? TwitterForMC.accessS : new MaskableTextFieldWidget(this.font, j, 140, i, 20, I18n.translate("tw.access.token.secret"), '●', 1000);
        TwitterForMC.accessS.x = j;
        TwitterForMC.accessS.setWidth(i);
        TwitterForMC.accessS.setMessage(I18n.translate("tw.access.token.secret"));
        this.addButton(TwitterForMC.accessS);

        if (flag) {
            TwitterForMC.update();
        }

        TwitterForMC.save = this.addButton(new CheckboxWidget(j, 170, 20, 20, I18n.translate("tw.save.keys"), TwitterForMC.save != null ? TwitterForMC.save.isChecked() : TwitterForMC.readToken()));

        TwitterForMC.autoLogin = this.addButton(new CheckboxWidget(j, 200, 20, 20, I18n.translate("tw.auto.login"), TwitterForMC.autoLogin != null ? TwitterForMC.autoLogin.isChecked() : TwitterForMC.readToken() && TwitterForMC.getToken().autoLogin()));

        TwitterForMC.login = TwitterForMC.login != null ? TwitterForMC.login : new ButtonWidget(0, this.height - 20, this.width / 2, 20, this.title.asFormattedString(), (b) -> {
            this.active(false);
            if (TwitterForMC.access.getText().isEmpty() || TwitterForMC.accessS.getText().isEmpty()) {
                this.pinLogin((twitter) -> {
                    this.minecraft.openScreen(TwitterForMC.twitterScreen);
                    this.addToast(twitter);
                });
            } else {
                this.simpleLogin((twitter) -> {
                    this.minecraft.openScreen(TwitterForMC.twitterScreen);
                    this.addToast(twitter);
                });
            }
            this.active(true);
        });
        TwitterForMC.login.y = this.height - 20;
        TwitterForMC.login.setWidth(k);
        TwitterForMC.login.setMessage(this.title.asFormattedString());
        this.addButton(TwitterForMC.login);

        this.addButton(new ButtonWidget(k, this.height - 20, k, 20, I18n.translate("tw.token.file.choose"), (b) -> {
            TwitterForMC.tokenFileChooser.choose();
        }));

        this.addButton(new ButtonWidget(k * 2, this.height - 20, k, 20, I18n.translate("gui.done"), (b) -> {
            this.onClose();
        }));
    }

    private void addToast(Twitter twitter) {
        try {
            this.minecraft.getToastManager().add(new TwitterNotificationToast(TwitterUtil.getInputStream(twitter.showUser(twitter.getId()).get400x400ProfileImageURLHttps()), I18n.translate("tw.login.successful"), null));
        } catch (Throwable t) {
            this.minecraft.getToastManager().add(new SystemToast(SystemToast.Type.TUTORIAL_HINT, new TranslatableText("tw.login.successful"), null));
        }
    }

    private void active(boolean flag) {
        TwitterForMC.consumer.setEditable(flag);
        TwitterForMC.consumerS.setEditable(flag);
        TwitterForMC.access.setEditable(flag);
        TwitterForMC.accessS.setEditable(flag);
        TwitterForMC.save.active = flag;
        TwitterForMC.autoLogin.active = flag;
        TwitterForMC.login.active = flag;
    }

    public void tick() {
        TwitterForMC.login.active = !(TwitterForMC.consumer.active && TwitterForMC.consumer.getText().isEmpty()) && !(TwitterForMC.consumerS.active && TwitterForMC.consumerS.getText().isEmpty());
        TwitterForMC.autoLogin.active = TwitterForMC.save.active && TwitterForMC.save.isChecked();
        TwitterForMC.autoLogin.visible = TwitterForMC.save.isChecked();
        super.tick();
    }

    public void render(int mouseX, int mouseY, float delta) {
        this.parent.render(-1, -1, delta);
        this.fillGradient(0, 0, this.width, this.height, -1072689136, -804253680);
        super.render(mouseX, mouseY, delta);
        if (TwitterForMC.save.isMouseOver(mouseX, mouseY)) {
            this.renderTooltip(this.font.wrapStringToWidthAsList(I18n.translate("tw.save.keys.desc"), this.width / 3), mouseX, mouseY);
        }
    }

    private synchronized void simpleLogin(Consumer<Twitter> callback) {
        try {
            Twitter twitter = new TwitterFactory().getInstance();
            twitter.setOAuthConsumer(TwitterForMC.consumer.getText(), TwitterForMC.consumerS.getText());
            AccessToken token = new AccessToken(TwitterForMC.access.getText(), TwitterForMC.accessS.getText());
            twitter.setOAuthAccessToken(token);
            twitter.getId();
            TwitterForMC.mctwitter = twitter;
            this.store(new Token(TwitterForMC.consumer.getText(), TwitterForMC.consumerS.getText(), token, TwitterForMC.autoLogin.isChecked()));
            callback.accept(TwitterForMC.mctwitter);
        } catch (Throwable e) {
            TwitterForMC.mctwitter = null;
            LOGGER.error("Error occurred while logging in twitter", e);
            this.minecraft.openScreen(new ErrorScreen(new TranslatableText("tw.login.failed"), this, e.getLocalizedMessage()));
        }
    }

    private synchronized void pinLogin(Consumer<Twitter> callback) {
        try {
            Twitter twitter = new TwitterFactory().getInstance();
            twitter.setOAuthConsumer(TwitterForMC.consumer.getText(), TwitterForMC.consumerS.getText());
            RequestToken requestToken = twitter.getOAuthRequestToken();
            Util.getOperatingSystem().open(new URI(requestToken.getAuthorizationURL()));
            this.minecraft.openScreen(new EnterPinScreen((pin) -> {
                try {
                    AccessToken token = twitter.getOAuthAccessToken(requestToken, pin);
                    twitter.setOAuthAccessToken(token);
                    twitter.getId();
                    TwitterForMC.mctwitter = twitter;
                    TwitterForMC.access.setText(token.getToken());
                    TwitterForMC.accessS.setText(token.getTokenSecret());
                    this.store(new Token(TwitterForMC.consumer.getText(), TwitterForMC.consumerS.getText(), token, TwitterForMC.autoLogin.isChecked()));
                    callback.accept(TwitterForMC.mctwitter);
                } catch (Throwable e) {
                    TwitterForMC.mctwitter = null;
                    LOGGER.error("Error occurred while logging in twitter", e);
                    this.minecraft.openScreen(new ErrorScreen(new TranslatableText("tw.login.failed"), this, e.getLocalizedMessage()));
                }
            }));
        } catch (Throwable e) {
            LOGGER.error("Error occurred while logging in twitter", e);
            this.minecraft.openScreen(new ErrorScreen(new TranslatableText("tw.login.failed"), this, e.getLocalizedMessage()));
        }
    }

    private synchronized void store(Token token) throws Throwable {
        if (TwitterForMC.save.isChecked()) {
            ObjectOutputStream var1 = null;
            try {
                var1 = new ObjectOutputStream(new FileOutputStream(TwitterForMC.getTokenFile()));
                var1.writeObject(token);
                var1.flush();
            } finally {
                IOUtils.closeQuietly(var1);
            }
        }
    }
}
