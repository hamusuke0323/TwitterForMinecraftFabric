package com.hamusuke.twitter4mc;

import com.google.common.collect.Sets;
import com.hamusuke.twitter4mc.emoji.EmojiManager;
import com.hamusuke.twitter4mc.gui.filechooser.FileChooserOpen;
import com.hamusuke.twitter4mc.gui.screen.TwitterScreen;
import com.hamusuke.twitter4mc.gui.widget.MaskableTextFieldWidget;
import com.hamusuke.twitter4mc.license.LicenseManager;
import com.hamusuke.twitter4mc.texture.TextureManager;
import com.hamusuke.twitter4mc.tweet.TweetSummary;
import com.hamusuke.twitter4mc.utils.NewToken;
import com.hamusuke.twitter4mc.utils.TwitterThread;
import com.hamusuke.twitter4mc.utils.TwitterUtil;
import com.hamusuke.twitter4mc.utils.VersionChecker;
import com.sun.javafx.application.PlatformImpl;
import javafx.application.Platform;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CheckboxWidget;
import net.minecraft.client.options.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;

import java.io.*;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.TreeSet;

@Environment(EnvType.CLIENT)
public final class TwitterForMC implements ClientModInitializer {
    public static final String MOD_ID = "twitter4mc";
    private static final Logger LOGGER = LogManager.getLogger();
    private static final TextureManager textureManager = new TextureManager();
    private static final EmojiManager emojiManager = new EmojiManager();
    @Nullable
    private static Path configFile;
    @Nullable
    private static File tokenFile;
    @Nullable
    public static Twitter mcTwitter;
    @Nullable
    private static NewToken token;
    public static final TwitterScreen twitterScreen = new TwitterScreen();
    public static boolean loginTwitter;
    public static final TreeSet<Status> tweets = Sets.newTreeSet(Collections.reverseOrder());
    public static final TreeSet<TweetSummary> tweetSummaries = Sets.newTreeSet(Collections.reverseOrder());
    public static KeyBinding openTwitter;
    @Nullable
    public static MaskableTextFieldWidget consumer;
    @Nullable
    public static MaskableTextFieldWidget consumerS;
    @Nullable
    public static MaskableTextFieldWidget access;
    @Nullable
    public static MaskableTextFieldWidget accessS;
    @Nullable
    public static CheckboxWidget save;
    @Nullable
    public static CheckboxWidget autoLogin;
    @Nullable
    public static ButtonWidget login;
    public static final FileChooserOpen tokenFileChooser = new FileChooserOpen((file) -> {
        if (file != null) {
            NewToken t = read(file);
            if (t != null) {
                token = t;
                update();
                if (token.autoLogin()) {
                    if (!save.isChecked()) {
                        save.onPress();
                    }
                    if (!autoLogin.isChecked()) {
                        autoLogin.onPress();
                    }
                } else {
                    if (autoLogin.isChecked()) {
                        autoLogin.onPress();
                    }
                }
            }
        }
    }, FabricLoader.getInstance().getGameDir().toFile());

    public void onInitializeClient() {
        LOGGER.info("Hello from TwitterForMC#onInitializeClient!");

        LicenseManager.registerLicense(new Identifier(TwitterForMC.MOD_ID, "license/mitlicense.txt"), 400, "tw.license.thismod");
        LicenseManager.registerLicense(new Identifier(TwitterForMC.MOD_ID, "license/twitter4j_license.txt"), 270, "tw.license.twitter4j");
        LicenseManager.registerLicense(new Identifier(TwitterForMC.MOD_ID, "license/twemoji_graphics_license.txt"), 320, "tw.license.twemoji.graphics");

        VersionChecker.checkUpdate();

        configFile = FabricLoader.getInstance().getConfigDir().resolve(MOD_ID);
        if (!configFile.toFile().exists()) {
            if (configFile.toFile().mkdir()) {
                LOGGER.info("made config directory: {}", MOD_ID);
            }
        }
        tokenFile = configFile.resolve("token").toFile();

        openTwitter = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.twitter4mc.opentw", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_B, "key.categories.gameplay"));
        ClientTickEvents.END_CLIENT_TICK.register((client) -> {
            while (openTwitter.wasPressed()) {
                twitterScreen.setParentScreen(null);
                client.openScreen(twitterScreen);
            }
        });

        ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(emojiManager);

        PlatformImpl.startup(() -> {
        });
        Platform.setImplicitExit(false);

        ClientLifecycleEvents.CLIENT_STOPPING.register((client) -> Platform.exit());

        new TwitterThread(() -> {
            token = read(tokenFile);
            if (token != null) {
                AccessToken var1 = new AccessToken(token.getAccessToken(), token.getAccessTokenSecret());
                if (token.autoLogin()) {
                    try {
                        mcTwitter = new TwitterFactory().getInstance();
                        mcTwitter.setOAuthConsumer(token.getConsumer(), token.getConsumerSecret());
                        mcTwitter.setOAuthAccessToken(var1);
                        mcTwitter.getId();
                        loginTwitter = true;
                        LOGGER.info("Successfully logged in twitter.");
                    } catch (Throwable e) {
                        mcTwitter = null;
                        loginTwitter = false;
                        LOGGER.error("Error occurred while logging in twitter", e);
                    }
                }
            }

            if (loginTwitter) {
                loadTimeline();
                for (Status s : tweets) {
                    tweetSummaries.add(new TweetSummary(s));
                }
            }
        }).start();
    }

    public static synchronized void saveTimeline() throws IOException {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(configFile.resolve("timeline").toFile()))) {
            oos.writeObject(tweets);
            oos.flush();
        } catch (IOException e) {
            LOGGER.error("Error occurred while saving timeline", e);
            throw e;
        }
    }

    private static synchronized void loadTimeline() {
        File file = configFile.resolve("timeline").toFile();
        if (!file.exists()) {
            return;
        }
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            Object object = ois.readObject();
            if (object instanceof List) {
                List<Status> statuses = (List<Status>) object;
                tweets.clear();
                tweets.addAll(statuses);
            } else {
                TreeSet<Status> statuses = (TreeSet<Status>) object;
                tweets.clear();
                tweets.addAll(statuses);
            }
        } catch (Throwable e) {
            LOGGER.error("Error occurred while reading timeline", e);
        }
    }

    @Nullable
    private static synchronized NewToken read(File tokenFile) {
        if (!tokenFile.exists()) {
            return null;
        }
        try {
            return TwitterUtil.readToken(tokenFile);
        } catch (Exception e) {
            LOGGER.info("Failed to read token. try to read old token");
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(tokenFile))) {
                Token old = (Token) ois.readObject();
                return TwitterUtil.oldTokenToNewTokenAndSave(old, tokenFile);
            } catch (Exception e1) {
                LOGGER.error("Error occurred while reading tokens", e);
            }
        }

        return null;
    }

    public static synchronized void store(@NotNull String consumer, @NotNull String consumerS, @NotNull AccessToken token, boolean autoLogin) throws Throwable {
        Objects.requireNonNull(consumer, "consumer cannot be null");
        Objects.requireNonNull(consumerS, "consumer secret cannot be null");
        Objects.requireNonNull(token, "access token cannot be null");

        if (TwitterForMC.save.isChecked()) {
            TwitterUtil.saveToken(new NewToken(consumer, consumerS, token, autoLogin), tokenFile);
        }
    }

    @Nullable
    public static File getTokenFile() {
        return tokenFile;
    }

    @Nullable
    public static Path getConfigFile() {
        return configFile;
    }

    @Nullable
    public static NewToken getToken() {
        return token;
    }

    public static boolean isAutoLogin() {
        return readToken() && token.autoLogin();
    }

    public static boolean readToken() {
        return token != null;
    }

    public static void update() {
        if (readToken()) {
            TwitterForMC.consumer.setText(token.getConsumer());
            TwitterForMC.consumerS.setText(token.getConsumerSecret());
            TwitterForMC.access.setText(token.getAccessToken());
            TwitterForMC.accessS.setText(token.getAccessTokenSecret());
        }
    }

    public static TextureManager getTextureManager() {
        return textureManager;
    }

    public static EmojiManager getEmojiManager() {
        return emojiManager;
    }
}
