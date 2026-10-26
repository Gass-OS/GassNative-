package com.gass.gassnative;


import android.speech.tts.TextToSpeech;
import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Handler;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.content.Context;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import java.net.HttpURLConnection;
import java.net.URL;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import org.json.JSONObject;


public class AiActivity extends Activity {
    // GASS_TTS_V1
    private TextToSpeech gassTts;
    private boolean gassTtsReady = false;

    // GASS_BOT_V1
    private final Handler botHandler = new Handler();
    private TextView botMessage, botVoiceButton;
    private RobotView botRobot;
    private boolean botVoiceEnabled = true;
    private boolean botHasGreeted = false;
    private String botPendingSpeech = null;
    private int botMessageIndex = 0;

    // GASS_OFFLINE_BRAIN_V2
    private final String[] botLines = {
        "Hey bro! I'm GASS BOT, your little offline companion. 🤖",
        "Linux tip: run apt update before installing packages.",
        "Linux tip: use apt search followed by a package name to find software.",
        "Linux tip: df -h shows free disk space on mounted filesystems.",
        "Linux tip: free -h gives you a readable memory overview.",
        "Linux tip: uname -a displays kernel and system information.",
        "Linux tip: pwd shows your current directory.",
        "Linux tip: ls -lah lists files, including hidden files.",
        "Linux tip: use cd to move between directories.",
        "Linux tip: read an error message carefully before changing your system.",
        "Linux tip: make a backup before editing important configuration files.",
        "Linux tip: use history to review commands you ran in the shell.",
        "Linux tip: man followed by a command can open its manual page.",
        "Linux tip: use grep to find matching text in source files.",
        "Linux tip: use tail to inspect the latest lines of a log.",
        "Android tip: keep API keys and server secrets out of your APK.",
        "Android tip: test your app after each important code change.",
        "Developer tip: change one thing at a time so bugs are easier to trace.",
        "Developer tip: a reproducible bug is much easier to fix.",
        "Developer tip: small working steps beat one giant risky rewrite.",
        "Developer tip: keep backups before applying automated patches.",
        "Developer tip: readable code saves time when you return to it later.",
        "Developer tip: build logs often reveal the first real error.",
        "Developer tip: test on the actual device, not only on paper.",
        "GassNative is an Android project that brings a Debian Linux desktop experience to phones.",
        "GassNative uses a manual Android build script rather than a Gradle build.",
        "The GassNative project is being developed by Aymen Gharsalli.",
        "Aymen works on GassNative and the Gass-OS software projects.",
        "GASS AI can provide built-in offline answers and use its online assistant when connected.",
        "Want to learn GassNative? Ask me about Debian, profiles, settings, or the desktop.",
        "Remember: GASS BOT's built-in tips work without internet access.",
        "Joke: why do programmers prefer dark mode? Because light attracts bugs! 😂",
        "Joke: there are 10 kinds of people: those who understand binary and those who don't.",
        "Joke: my Linux computer never gets lost. It always knows its path. 😄",
        "Joke: a programmer's favourite place is the Foo Bar.",
        "Joke: I tried to catch a bug, but it turned into a feature.",
        "Joke: why did the developer go broke? Too many cache misses.",
        "Joke: my robot has great manners. It always says please and thank you-byte.",
        "Joke: the computer needed glasses because it couldn't C clearly.",
        "Motivation: every expert started as a beginner who kept going.",
        "Motivation: fixing one bug today is real progress.",
        "Motivation: don't fear errors; use them as clues.",
        "Motivation: keep learning, keep testing, and keep building. 💙",
        "Need help? Ask me to explain a Linux command or an Android error.",
        "I am software, not a human, but I'm here to help you build and learn.",
        "If a tip doesn't fit your setup, check your Debian version before running commands."
    };

    private final Runnable botTicker = new Runnable() {
        @Override public void run() {
            if (!botHasGreeted || isFinishing() || isDestroyed()) return;
            showBotMessage(true);
            botHandler.postDelayed(this, 90000L);
        }
    };

    private final int bg = 0xFF080E1D;
    private final int panel = 0xFF111D35;
    private final int white = 0xFFF4F7FF;
    private final int soft = 0xFF9FB0D6;
    private final int blue = 0xFF60A5FA;
    private final int green = 0xFF6EE7B7;

    private static final String AI_ENDPOINT = "https://ptigtwzaeiydraqrtzwd.supabase.co/functions/v1/smooth-action";
    private static final String SUPABASE_PUBLISHABLE_KEY = "sb_publishable_ed3B12WA0AYUf8QtGQNDWg_FmNXpt_P";
    private static final String SUPABASE_ANON_JWT = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InB0aWd0d3phZWl5ZHJhcXJ0endkIiwicm9sZSI6ImFub24iLCJpYXQiOjE3OTI5ODM5NjgsImV4cCI6MjEwODU1OTk2OH0.fAWoJofk0mUmBf-GhrOA6-zIF_2eXkl3ZdOTvWWUMow";

    private LinearLayout messages;
    private ScrollView scroll;
    private EditText input;
    private LinearLayout suggestionRow;
    // GASS_CONVERSATION_V1
    private String previousUserMessage = "";
    private String lastUserMessage = "";
    private String lastAssistantReply = "";
    private String pendingContext = "";
    private String[] pendingChoices = null;

    private int dp(float n) {
        return (int)(n * getResources().getDisplayMetrics().density + .5f);
    }

    private GradientDrawable shape(int color, int stroke, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radius));
        if (stroke != 0) d.setStroke(dp(1), stroke);
        return d;
    }

    private TextView label(String s, int size, int color) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(size);
        t.setTextColor(color);
        return t;
    }

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);

        // GASS_TTS_V1
        gassTts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS && gassTts != null) {
                int result = gassTts.setLanguage(Locale.US);
                gassTtsReady =
                    result != TextToSpeech.LANG_MISSING_DATA &&
                    result != TextToSpeech.LANG_NOT_SUPPORTED;

                // Prefer an installed English male-sounding voice when available.
                if (gassTtsReady && android.os.Build.VERSION.SDK_INT >= 21) {
                    try {
                        java.util.Set<android.speech.tts.Voice> voices =
                            gassTts.getVoices();
                        android.speech.tts.Voice maleVoice = null;
                        if (voices != null) {
                            for (android.speech.tts.Voice voice : voices) {
                                if (voice == null || voice.getLocale() == null) continue;
                                Locale voiceLocale = voice.getLocale();
                                if (!"en".equalsIgnoreCase(voiceLocale.getLanguage())) continue;
                                String voiceName = voice.getName().toLowerCase(Locale.ROOT);
                                if (voiceName.contains("male")
                                    || voiceName.contains("david")
                                    || voiceName.contains("mark")
                                    || voiceName.contains("guy")) {
                                    maleVoice = voice;
                                    break;
                                }
                            }
                        }
                        if (maleVoice != null) {
                            gassTts.setVoice(maleVoice);
                        }
                    } catch (Exception ignored) {
                        // Keep the default English voice if voice selection fails.
                    }
                }

                if (gassTtsReady) {
                    gassTts.setSpeechRate(1.0f);
                    if (botPendingSpeech != null && botVoiceEnabled) {
                        String pendingSpeech = botPendingSpeech;
                        botPendingSpeech = null;
                        speakReply(pendingSpeech);
                    }
                }
            }
        });

        getWindow().setStatusBarColor(bg);
        getWindow().setNavigationBarColor(bg);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(12), dp(16), dp(12));
        root.setBackgroundColor(bg);
        setContentView(root);

        TextView back = label("‹  GASSNATIVE", 14, blue);
        back.setTypeface(null, Typeface.BOLD);
        back.setPadding(dp(4), dp(8), 0, dp(12));
        back.setOnClickListener(v -> finish());
        root.addView(back);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setPadding(dp(2), dp(8), 0, dp(16));

        TextView title = label("Gass AI", 30, white);
        title.setTypeface(null, Typeface.BOLD);
        header.addView(title);
        header.addView(label("Your device. Your intelligence.", 13, soft));

        TextView status = label("●  OFFLINE MINI AI  ·  RULE-BASED", 11, green);
        status.setPadding(0, dp(10), 0, 0);
        header.addView(status);
        root.addView(header);

        // GASS_BOT_V1: native-drawn animated robot, no image assets.
        LinearLayout botCard = new LinearLayout(this);
        botCard.setOrientation(LinearLayout.HORIZONTAL);
        botCard.setGravity(Gravity.CENTER_VERTICAL);
        botCard.setPadding(dp(8), dp(8), dp(10), dp(8));
        botCard.setBackground(shape(0xFF101C34, 0xFF31547E, 20));

        botRobot = new RobotView(this);
        botCard.addView(botRobot, new LinearLayout.LayoutParams(dp(106), dp(112)));

        LinearLayout botInfo = new LinearLayout(this);
        botInfo.setOrientation(LinearLayout.VERTICAL);
        botInfo.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams botInfoParams =
            new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        botInfoParams.leftMargin = dp(5);
        botCard.addView(botInfo, botInfoParams);

        TextView botName = label("GASS BOT  •  ONLINE", 12, blue);
        botName.setTypeface(null, Typeface.BOLD);
        botInfo.addView(botName);

        botMessage = label("Hey bro! I'm your little AI robot. 🤖", 12, white);
        botMessage.setPadding(0, dp(5), 0, dp(7));
        botInfo.addView(botMessage);

        LinearLayout botActions = new LinearLayout(this);
        botActions.setGravity(Gravity.CENTER_VERTICAL);
        botInfo.addView(botActions);

        TextView botTalkButton = label("TELL ME SOMETHING  ↗", 10, green);
        botTalkButton.setTypeface(null, Typeface.BOLD);
        botTalkButton.setPadding(dp(9), dp(7), dp(9), dp(7));
        botTalkButton.setBackground(shape(0xFF19334A, 0xFF326A78, 14));
        botActions.addView(botTalkButton);
        botTalkButton.setOnClickListener(v -> showBotMessage(true));

        botVoiceButton = label("🔊", 14, white);
        botVoiceButton.setGravity(Gravity.CENTER);
        botVoiceButton.setContentDescription("Toggle GASS BOT voice");
        botVoiceButton.setPadding(dp(11), dp(5), dp(7), dp(5));
        botActions.addView(botVoiceButton);
        botVoiceButton.setOnClickListener(v -> {
            botVoiceEnabled = !botVoiceEnabled;
            botVoiceButton.setText(botVoiceEnabled ? "🔊" : "🔇");
            if (!botVoiceEnabled) {
                botPendingSpeech = null;
                if (gassTts != null) gassTts.stop();
            } else {
                speakReply("Voice is on, bro!");
            }
        });

        LinearLayout.LayoutParams botCardParams =
            new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        botCardParams.bottomMargin = dp(10);
        root.addView(botCard, botCardParams);


        scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setClipToPadding(false);

        messages = new LinearLayout(this);
        messages.setOrientation(LinearLayout.VERTICAL);
        messages.setPadding(0, dp(6), 0, dp(12));
        scroll.addView(messages);

        root.addView(scroll, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        LinearLayout composer = new LinearLayout(this);
        composer.setGravity(Gravity.CENTER_VERTICAL);
        composer.setPadding(dp(8), dp(5), dp(6), dp(5));
        composer.setBackground(shape(panel, 0xFF344B73, 22));

        input = new EditText(this);
        input.setSingleLine(true);
        input.setTextSize(15);
        input.setTextColor(white);
        input.setHintTextColor(soft);
        input.setHint("Message Gass AI...");
        input.setPadding(dp(10), dp(8), dp(8), dp(8));
        input.setBackgroundColor(Color.TRANSPARENT);
        input.setImeOptions(EditorInfo.IME_ACTION_SEND);
        input.setInputType( android.text.InputType.TYPE_CLASS_TEXT
            | android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        composer.addView(input, new LinearLayout.LayoutParams(
            0, dp(48), 1));

        TextView send = label("Send  ↑", 14, Color.WHITE);
        send.setGravity(Gravity.CENTER);
        send.setTypeface(null, Typeface.BOLD);
        send.setBackground(shape(0xFF2563EB, 0, 17));
        composer.addView(send, new LinearLayout.LayoutParams(dp(82), dp(44)));

        // GASS_AUTOCOMPLETE_V1
        suggestionRow = new LinearLayout(this);
        suggestionRow.setOrientation(LinearLayout.HORIZONTAL);
        suggestionRow.setGravity(Gravity.CENTER_VERTICAL);
        suggestionRow.setPadding(dp(8), dp(2), dp(8), dp(4));
        root.addView(suggestionRow, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT));

        root.addView(composer, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // GASS_BROWSER_ASSISTANT_V1
        LinearLayout browserRow = new LinearLayout(this);
        browserRow.setGravity(Gravity.CENTER);
        browserRow.setPadding(dp(4), dp(4), dp(4), dp(8));

        TextView googleButton = label("Google", 13, white);
        googleButton.setGravity(Gravity.CENTER);
        googleButton.setBackground(shape(panel, 0xFF344B73, 14));
        browserRow.addView(googleButton,
            new LinearLayout.LayoutParams(0, dp(42), 1));

        TextView chatGptButton = label("Ask ChatGPT", 13, white);
        chatGptButton.setGravity(Gravity.CENTER);
        chatGptButton.setBackground(shape(0xFF166534, 0, 14));
        LinearLayout.LayoutParams chatParams =
            new LinearLayout.LayoutParams(0, dp(42), 1);
        chatParams.setMargins(dp(6), 0, dp(6), 0);
        browserRow.addView(chatGptButton, chatParams);

        TextView pasteButton = label("Paste Answer", 13, white);
        pasteButton.setGravity(Gravity.CENTER);
        pasteButton.setBackground(shape(0xFF2563EB, 0, 14));
        browserRow.addView(pasteButton,
            new LinearLayout.LayoutParams(0, dp(42), 1));

        root.addView(browserRow, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT));

        googleButton.setOnClickListener(v -> openGoogleSearch());
        chatGptButton.setOnClickListener(v -> openChatGPT());
        pasteButton.setOnClickListener(v -> pasteBrowserAnswer());

        input.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(
                    CharSequence text, int start, int count, int after) {}
            @Override public void onTextChanged(
                    CharSequence text, int start, int before, int count) {
                updateSuggestions(text.toString());
            }
            @Override public void afterTextChanged(android.text.Editable e) {}
        });

        send.setOnClickListener(v -> sendMessage());
        input.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND
                || actionId == EditorInfo.IME_ACTION_DONE) {
                sendMessage();
                return true;
            }
            return false;
        });

        addMessage("GASS AI",
            "Hi! I'm GassNative Mini AI. 💙\n"
            + "I'm running locally on your device. How can I help?",
            false);
    }


    private String expandChoice(String raw) {
        String value = raw.trim();
        if (!value.matches("[1-4]") || pendingChoices == null) return raw;

        int index = Integer.parseInt(value) - 1;
        if (index < 0 || index >= pendingChoices.length) return raw;

        String choice = pendingChoices[index];
        pendingChoices = null;

        if (pendingContext.toLowerCase(Locale.ROOT).contains("gassnative")
                || pendingContext.toLowerCase(Locale.ROOT).contains("debian")
                || pendingContext.toLowerCase(Locale.ROOT).contains("linux")) {
            return pendingContext + " " + choice;
        }
        return choice;
    }

    private String clarificationMenu(String question) {
        String q = question.toLowerCase(Locale.ROOT);
        boolean technical = q.contains("gassnative")
            || q.contains("debian") || q.contains("linux")
            || q.contains("desktop") || q.contains("apk")
            || q.contains("computer") || q.contains("setting");

        pendingContext = lastUserMessage;
        if (technical) {
            pendingChoices = new String[] {
                "installation and setup",
                "desktop and XFCE",
                "settings and configuration",
                "troubleshooting and errors"
            };
            return "Of course, bro 😎💙 What part do you mean? Pick one:\n\n"
                + "1. Installation and setup\n"
                + "2. Desktop and XFCE\n"
                + "3. Settings and configuration\n"
                + "4. Troubleshooting and errors\n\n"
                + "Reply with a number, or tell me more in your own words.";
        }

        pendingChoices = new String[] {
            "explain it simply",
            "give me step-by-step instructions",
            "show me an example",
            "explain the advantages and disadvantages"
        };
        return "I can help, bro 💙 Which kind of answer would you like?\n\n"
            + "1. Explain it simply\n"
            + "2. Step-by-step instructions\n"
            + "3. An example\n"
            + "4. Advantages and disadvantages\n\n"
            + "Reply with a number, or give me a little more context.";
    }

    private boolean isClarificationRequest(String s) {
        return s.matches("^(what do you mean|what does that mean|"
            + "explain that|explain it|i don't understand|"
            + "i dont understand|huh|what about that|"
            + "can you explain)( bro)?$");
    }

    private boolean isIncompleteQuestion(String s) {
        return s.matches("^(what('?s| is)?|what about|tell me about|"
            + "explain|help me with|how do i|how can i|"
            + "can you help)( bro)?$");
    }


    // GASS_BROWSER_ASSISTANT_V1
    private String getBrowserQuestion() {
        String question = lastUserMessage == null ? "" : lastUserMessage.trim();
        if (question.isEmpty() && input != null) {
            question = input.getText().toString().trim();
        }
        return question;
    }

    private void openGoogleSearch() {
        String question = getBrowserQuestion();
        if (question.isEmpty()) {
            input.setError("Type a question first");
            input.requestFocus();
            return;
        }

        try {
            android.content.Intent intent = new android.content.Intent(
                android.content.Intent.ACTION_VIEW,
                android.net.Uri.parse(
                    "https://www.google.com/search?q="
                    + android.net.Uri.encode(question)));
            startActivity(intent);
        } catch (Exception e) {
            android.widget.Toast.makeText(this,
                "Could not open Google.", android.widget.Toast.LENGTH_SHORT).show();
        }
    }

    private void openChatGPT() {
        String question = getBrowserQuestion();
        if (question.isEmpty()) {
            input.setError("Type a question first");
            input.requestFocus();
            return;
        }

        android.content.ClipboardManager clipboard =
            (android.content.ClipboardManager)
                getSystemService(CLIPBOARD_SERVICE);
        if (clipboard != null) {
            clipboard.setPrimaryClip(android.content.ClipData.newPlainText(
                "GASS AI question", question));
        }

        try {
            String url = "https://aistudio.google.com/prompts/new_chat?prompt="
                + android.net.Uri.encode(question);
            android.content.Intent intent = new android.content.Intent(
                android.content.Intent.ACTION_VIEW,
                android.net.Uri.parse(url));
            startActivity(intent);

            android.widget.Toast.makeText(this,
                "AI Studio opened. Check the question and tap Run. Copy its answer, then return to GASS AI.",
                android.widget.Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            android.widget.Toast.makeText(this,
                "Could not open Google AI Studio.",
                android.widget.Toast.LENGTH_SHORT).show();
        }
    }

    private void pasteBrowserAnswer() {
        android.content.ClipboardManager clipboard =
            (android.content.ClipboardManager)
                getSystemService(CLIPBOARD_SERVICE);

        if (clipboard == null || !clipboard.hasPrimaryClip()
                || clipboard.getPrimaryClip() == null
                || clipboard.getPrimaryClip().getItemCount() == 0) {
            android.widget.Toast.makeText(this,
                "Copy an answer first, then tap Paste Answer.",
                android.widget.Toast.LENGTH_LONG).show();
            return;
        }

        CharSequence copied = clipboard.getPrimaryClip()
            .getItemAt(0).coerceToText(this);

        if (copied == null || copied.toString().trim().isEmpty()) {
            android.widget.Toast.makeText(this,
                "The clipboard is empty.", android.widget.Toast.LENGTH_SHORT).show();
            return;
        }

        input.setText(copied.toString());
        input.setSelection(input.length());
        input.requestFocus();

        android.widget.Toast.makeText(this,
            "Answer pasted into the message box. Tap Send to continue.",
            android.widget.Toast.LENGTH_LONG).show();
    }


    // GASS_AUTOCOMPLETE_V1
    private String[] offlineDictionary() {
        return new String[] {
            "GassNative installation",
            "GassNative features",
            "GassNative settings",
            "Debian installation failed",
            "Debian root filesystem",
            "Linux terminal commands",
            "Linux package installation",
            "XFCE desktop troubleshooting",
            "XFCE panel missing",
            "GPU acceleration",
            "GPU troubleshooting",
            "Why is llvmpipe running?",
            "Freedreno graphics driver",
            "Vulkan troubleshooting",
            "Wayland display",
            "X11 display troubleshooting",
            "PRoot explained",
            "DNS troubleshooting",
            "Internet connection problem",
            "Android storage access",
            "APK build failed",
            "APK installation error",
            "Supabase connection error",
            "AI offline knowledge",
            "Free online AI models",
            "Open Google AI Studio",
            "Paste an AI answer",
            "Linux permissions",
            "Linux environment variables",
            "Backup my Linux files",
            "Check CPU and memory",
            "Explain this error",
            "Show step-by-step instructions",
            "Give me an example",
            "Help me with GassNative"
        };
    }

    private void updateSuggestions(String raw) {
        if (suggestionRow == null) return;
        suggestionRow.removeAllViews();

        String text = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
        java.util.ArrayList<String> matches = new java.util.ArrayList<>();

        if (text.isEmpty()) {
            matches.add("Help me");
            matches.add("GPU acceleration");
            matches.add("Debian install");
        } else if (text.contains("gpu") || text.contains("llvmpipe")
                || text.contains("vulkan") || text.contains("opengl")
                || text.contains("graphics") || text.contains("acceleration")) {
            matches.add("Fix llvmpipe");
            matches.add("Check Vulkan GPU");
            matches.add("GPU acceleration");
        } else if (text.contains("debian") || text.contains("tar")
                || text.contains("rootfs") || text.contains("extract")
                || text.contains("install")) {
            matches.add("Fix tar extraction");
            matches.add("Install Debian");
            matches.add("Check rootfs");
        } else if (text.contains("xfce") || text.contains("desktop")
                || text.contains("display") || text.contains("wayland")
                || text.contains("black screen")) {
            matches.add("Fix black screen");
            matches.add("XFCE desktop");
            matches.add("Wayland display");
        } else if (text.contains("network") || text.contains("internet")
                || text.contains("dns") || text.contains("connection")) {
            matches.add("Fix DNS");
            matches.add("Check internet");
            matches.add("Network troubleshooting");
        } else if (text.contains("build") || text.contains("javac")
                || text.contains("apk") || text.contains("compile")
                || text.contains("error") || text.contains("exception")) {
            matches.add("Fix APK build");
            matches.add("Show javac error");
            matches.add("Check Android SDK");
        } else if (text.contains("help") || text.contains("support")
                || text.contains("assist")) {
            matches.add("Help me");
            matches.add("GassNative features");
            matches.add("Troubleshoot an error");
        }

        // Add existing dictionary suggestions without duplicates.
        if (matches.size() < 3 && !text.isEmpty()) {
            for (String phrase : offlineDictionary()) {
                String candidate = phrase.toLowerCase(Locale.ROOT);
                if ((candidate.startsWith(text) || candidate.contains(text))
                        && !candidate.equals(text) && !matches.contains(phrase)) {
                    matches.add(phrase);
                }
                if (matches.size() >= 3) break;
            }
        }

        if (matches.isEmpty()) {
            suggestionRow.setVisibility(View.GONE);
            return;
        }

        suggestionRow.setVisibility(View.VISIBLE);
        for (int i = 0; i < matches.size() && i < 3; i++) {
            final String suggestion = matches.get(i);
            TextView chip = label(suggestion, 11, white);
            chip.setGravity(Gravity.CENTER);
            chip.setPadding(dp(8), dp(8), dp(8), dp(8));
            chip.setBackground(shape(panel, 0xFF344B73, 16));

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
            if (i > 0) params.setMargins(dp(4), 0, 0, 0);
            suggestionRow.addView(chip, params);

            chip.setOnClickListener(v -> {
                input.setText(suggestion);
                input.setSelection(input.length());
                input.requestFocus();
            });
        }
    }

    private void showBotMessage(boolean speak) {
        if (botMessage == null || botLines.length == 0) return;
        String line = botLines[botMessageIndex % botLines.length];
        botMessageIndex++;
        botMessage.setText(line);
        if (botRobot != null) botRobot.setTalking(speak && botVoiceEnabled);

        if (speak && botVoiceEnabled) {
            if (gassTtsReady && gassTts != null) {
                speakReply(line);
            } else {
                botPendingSpeech = line;
            }
        }

        if (botRobot != null) {
            botRobot.postDelayed(() -> {
                if (botRobot != null) botRobot.setTalking(false);
            }, 3500L);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!botHasGreeted) {
            botHasGreeted = true;
            showBotMessage(true);
        }
        botHandler.removeCallbacks(botTicker);
        botHandler.postDelayed(botTicker, 90000L);
    }

    @Override
    protected void onPause() {
        botHandler.removeCallbacks(botTicker);
        if (botRobot != null) botRobot.setTalking(false);
        super.onPause();
    }

    private final class RobotView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF rect = new RectF();
        private boolean talking = false;

        RobotView(Context context) {
            super(context);
            setLayerType(View.LAYER_TYPE_SOFTWARE, null);
            setContentDescription("Animated GASS BOT with blinking blue eyes");
        }

        void setTalking(boolean value) {
            talking = value;
            invalidate();
        }

        private void fill(Canvas c, int color) {
            paint.setColor(color);
            paint.setStyle(Paint.Style.FILL);
            paint.clearShadowLayer();
        }

        private void round(Canvas c, float l, float t, float r, float b,
                           float radius, int color) {
            fill(c, color);
            rect.set(l, t, r, b);
            c.drawRoundRect(rect, radius, radius, paint);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            long now = System.currentTimeMillis();
            float scale = Math.min(getWidth() / 112f, getHeight() / 116f);
            if (scale <= 0) return;

            canvas.save();
            canvas.translate((getWidth() - 112f * scale) / 2f,
                             (getHeight() - 116f * scale) / 2f);
            canvas.scale(scale, scale);

            float bob = (float)Math.sin(now / 380.0) * 2.2f;
            canvas.translate(0, bob);

            // Antenna and cyan tip.
            round(canvas, 53, 5, 59, 20, 3, 0xFF7297C8);
            fill(canvas, 0xFF62E8FF);
            canvas.drawCircle(56, 5, 5, paint);

            // Little arms.
            round(canvas, 9, 53, 26, 75, 8, 0xFF294B78);
            round(canvas, 86, 53, 103, 75, 8, 0xFF294B78);
            fill(canvas, 0xFF5DDCFF);
            canvas.drawCircle(17, 72, 4, paint);
            canvas.drawCircle(95, 72, 4, paint);

            // Body.
            round(canvas, 25, 64, 87, 108, 15, 0xFF142B4C);
            round(canvas, 31, 69, 81, 102, 11, 0xFF203F68);
            round(canvas, 43, 77, 69, 92, 5, 0xFF0B1930);
            fill(canvas, talking ? 0xFF6EE7B7 : 0xFF5CDFFF);
            canvas.drawCircle(49, 84, 2.5f, paint);
            canvas.drawCircle(63, 84, 2.5f, paint);
            round(canvas, 49, 96, 63, 99, 1.5f, 0xFF5CDFFF);

            // Feet.
            round(canvas, 33, 103, 49, 114, 5, 0xFF294B78);
            round(canvas, 63, 103, 79, 114, 5, 0xFF294B78);
            round(canvas, 35, 109, 47, 112, 1.5f, 0xFF5CDFFF);
            round(canvas, 65, 109, 77, 112, 1.5f, 0xFF5CDFFF);

            // Head shell.
            round(canvas, 13, 18, 99, 72, 19, 0xFF294B78);
            round(canvas, 17, 21, 95, 68, 16, 0xFF142743);
            round(canvas, 22, 27, 90, 63, 12, 0xFF071426);

            // Side ear panels.
            round(canvas, 8, 35, 17, 52, 4, 0xFF4E9DDB);
            round(canvas, 95, 35, 104, 52, 4, 0xFF4E9DDB);

            // Blink briefly every few seconds.
            long phase = now % 4300L;
            boolean blink = phase > 3900L && phase < 4020L;
            float eyeHeight = blink ? 2.5f : 10f;
            float eyeY = 43f;

            // Eyes with a soft cyan halo.
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(0x335DEBFF);
            paint.setShadowLayer(8, 0, 0, 0xAA36CFFF);
            canvas.drawRoundRect( rectFor(29, eyeY-eyeHeight/2, 52, eyeY+eyeHeight/2),
                                  7, 7, paint);
            canvas.drawRoundRect( rectFor(60, eyeY-eyeHeight/2, 83, eyeY+eyeHeight/2),
                                  7, 7, paint);
            paint.clearShadowLayer();

            fill(canvas, blink ? 0xFF3BA9D0 : 0xFF65E9FF);
            canvas.drawRoundRect(rectFor(30, eyeY-eyeHeight/2, 51, eyeY+eyeHeight/2),
                                 7, 7, paint);
            canvas.drawRoundRect(rectFor(61, eyeY-eyeHeight/2, 82, eyeY+eyeHeight/2),
                                 7, 7, paint);

            if (!blink) {
                float look = (float)Math.sin(now / 850.0) * 2.5f;
                fill(canvas, 0xFF102440);
                canvas.drawCircle(40 + look, eyeY, 3.1f, paint);
                canvas.drawCircle(71 + look, eyeY, 3.1f, paint);
                fill(canvas, 0xFFFFFFFF);
                canvas.drawCircle(39 + look, eyeY - 1.5f, 1.2f, paint);
                canvas.drawCircle(70 + look, eyeY - 1.5f, 1.2f, paint);
            }

            // Mouth animates when speech is active.
            fill(canvas, 0xFF5DE4FF);
            if (talking && (now / 150L) % 2 == 0) {
                round(canvas, 49, 55, 63, 60, 2, 0xFF5DE4FF);
            } else {
                round(canvas, 51, 56, 61, 58, 2, 0xFF5DE4FF);
            }

            canvas.restore();
            postInvalidateDelayed(70L);
        }

        private RectF rectFor(float l, float t, float r, float b) {
            rect.set(l, t, r, b);
            return rect;
        }
    }

    // GASS_TTS_V1
    private String cleanTextForSpeech(String text) {
        if (text == null) return "";
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < text.length();) {
            int cp = text.codePointAt(i);
            i += Character.charCount(cp);
            boolean emoji =
                (cp >= 0x1F000 && cp <= 0x1FAFF) ||
                (cp >= 0x1FC00 && cp <= 0x1FFFF) ||
                (cp >= 0x2600 && cp <= 0x27BF) ||
                (cp >= 0x2300 && cp <= 0x23FF) ||
                (cp >= 0x1F1E6 && cp <= 0x1F1FF) ||
                (cp >= 0x1F3FB && cp <= 0x1F3FF) ||
                (cp >= 0xE0020 && cp <= 0xE007F) ||
                cp == 0xFE0E || cp == 0xFE0F || cp == 0x200D;
            if (!emoji) out.appendCodePoint(cp);
        }
        return out.toString().replaceAll("[`*_#]+", "")
            .replaceAll("\\s+", " ").trim();
    }

    private void speakReply(String text) {
        if (!gassTtsReady || gassTts == null || text == null) return;
        String spoken = cleanTextForSpeech(text);
        if (spoken.isEmpty()) return;
        gassTts.speak(spoken, TextToSpeech.QUEUE_FLUSH, null, "gass-reply");
    }

    private void sendMessage() {
        String rawMessage = input.getText().toString().trim();
        if (rawMessage.length() == 0) return;

        String message = expandChoice(rawMessage);
        previousUserMessage = lastUserMessage;
        lastUserMessage = message;

        input.setText("");
        addMessage("YOU", rawMessage, true);
        addMessage("GASS AI", "Connecting to GASS AI…", false);
        final View pending = messages.getChildAt(messages.getChildCount() - 1);

        InputMethodManager imm = (InputMethodManager)
            getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(input.getWindowToken(), 0);
        input.clearFocus();

        new Thread(() -> {
            String online = requestOnlineReply(message);
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                messages.removeView(pending);
                if (online != null && !online.trim().isEmpty()) {
                    lastAssistantReply = online.trim();
                    addMessage("GASS AI · ONLINE", lastAssistantReply, false);
                    speakReply(lastAssistantReply);
                } else {
                    lastAssistantReply = reply(message);
                    addMessage("GASS AI · OFFLINE", lastAssistantReply, false);
                    speakReply(lastAssistantReply);
                }
            });
        }, "GassAI-Request").start();
    }

    private String requestOnlineReply(String message) {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(AI_ENDPOINT);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(30000);
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            connection.setRequestProperty("apikey", SUPABASE_PUBLISHABLE_KEY);
            connection.setRequestProperty("Authorization", "Bearer " + SUPABASE_ANON_JWT);

            byte[] body = new JSONObject().put("message", message)
                .toString().getBytes(StandardCharsets.UTF_8);
            connection.setFixedLengthStreamingMode(body.length);
            try (java.io.OutputStream out = connection.getOutputStream()) {
                out.write(body);
            }

            int status = connection.getResponseCode();
            InputStream stream = status >= 200 && status < 300
                ? connection.getInputStream() : connection.getErrorStream();
            if (stream == null) return null;

            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            try (InputStream in = stream) {
                byte[] chunk = new byte[4096];
                int count;
                while ((count = in.read(chunk)) != -1) buffer.write(chunk, 0, count);
            }

            JSONObject result = new JSONObject(
                new String(buffer.toByteArray(), StandardCharsets.UTF_8));
            if (status < 200 || status >= 300) return null;

            String reply = result.optString("reply", "").trim();
            return reply.isEmpty() ? null : reply;
        } catch (Exception e) {
            android.util.Log.w("GASS_AI", "Online request failed: " + e.getClass().getSimpleName());
            return null;
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private void addMessage(String sender, String body, boolean user) {
        LinearLayout group = new LinearLayout(this);
        group.setOrientation(LinearLayout.VERTICAL);
        group.setGravity(user ? Gravity.RIGHT : Gravity.LEFT);
        group.setPadding(dp(2), dp(6), dp(2), dp(6));

        TextView who = label(sender, 10, user ? blue : green);
        who.setTypeface(null, Typeface.BOLD);
        who.setPadding(dp(8), 0, dp(8), dp(5));
        group.addView(who);

        TextView bubble = label(body, 15, white);
        bubble.setPadding(dp(14), dp(11), dp(14), dp(11));
        bubble.setLineSpacing(dp(2), 1.0f);
        bubble.setBackground(shape(
            user ? 0xFF244D91 : panel,
            user ? 0xFF386BC0 : 0xFF293B5A, 18));
        bubble.setMaxWidth((int)(getResources().getDisplayMetrics()
            .widthPixels * 0.78f));

        group.addView(bubble, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT));
        messages.addView(group);

        scroll.post(() -> scroll.fullScroll(View.FOCUS_DOWN));
    }


    // OFFLINE_KNOWLEDGE_V2
    private boolean offlineHasWord(String text, String word) {
        return java.util.regex.Pattern.compile(
            "(?<![a-z0-9])" + java.util.regex.Pattern.quote(word) +
            "(?![a-z0-9])"
        ).matcher(text).find();
    }

    private String searchOfflineKnowledge(String question) {
        String q = question.toLowerCase(Locale.ROOT)
            .replaceAll("[^a-z0-9._/ -]", " ")
            .replaceAll("\\s+", " ").trim();
        if (q.length() < 3) return null;

        String[] stop = {
            "the","and","for","with","what","how","why","can","you",
            "your","are","is","does","this","that","from","into",
            "about","please","help","tell","me","my","i","a","an",
            "to","of","in","on","it","do","could","would","should",
            "give","show","explain","find","need","want","have","has",
            "was","were","am","be","been","there","which","when",
            "where","who","much","many","some"
        };
        java.util.HashSet<String> ignored =
            new java.util.HashSet<>(java.util.Arrays.asList(stop));
        java.util.HashSet<String> terms = new java.util.HashSet<>();
        for (String word : q.split(" ")) {
            if (word.length() >= 2 && !ignored.contains(word)) terms.add(word);
        }
        if (terms.isEmpty()) return null;

        String[][] groups = {
            {"gpu","graphics","rendering","renderer","llvmpipe","opengl","vulkan","zink","freedreno","acceleration","slow","lag","lagging","performance"},
            {"install","installation","setup","configure","configuration","download","extract","extraction","tar","archive","rootfs"},
            {"error","errors","failed","failure","broken","problem","problems","fix","troubleshoot","debug","debugging","crash","crashing"},
            {"internet","network","online","offline","connection","connectivity","dns","domain","hostname","wifi"},
            {"apt","package","packages","software","repository","repositories","dependencies","dependency","dpkg"},
            {"storage","space","disk","full","capacity","filesystem"},
            {"memory","ram","oom","swap","process","cpu","hang","hanging"},
            {"desktop","xfce","session","display","screen","black","x11","xwayland","wayland","panel"},
            {"android","termux","apk","abi","arm64","aarch64","native","library","libraries"},
            {"api","http","https","rest","json","request","response","timeout","supabase","key","token","secret","authentication"},
            {"permission","permissions","chmod","chown","executable","denied","access"},
            {"path","directory","directories","folder","folders","file","files","pwd","ls"},
            {"git","commit","branch","push","pull","reset"}
        };

        java.util.HashSet<String> expanded = new java.util.HashSet<>(terms);
        for (String[] group : groups) {
            boolean matched = false;
            for (String alias : group) {
                if (terms.contains(alias)) { matched = true; break; }
            }
            if (matched) {
                for (String alias : group) expanded.add(alias);
            }
        }

        String best = null;
        int bestScore = 0;

        try {
            String[] files = getAssets().list("ai-knowledge");
            if (files == null) return null;

            for (String file : files) {
                if (!file.endsWith(".txt")) continue;
                StringBuilder data = new StringBuilder();
                try (java.io.BufferedReader reader =
                         new java.io.BufferedReader(
                             new java.io.InputStreamReader(
                                 getAssets().open("ai-knowledge/" + file),
                                 StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        data.append(line).append('\n');
                    }
                }

                for (String article : data.toString().split("\\n\\s*\\n")) {
                    String text = article.trim();
                    if (text.length() < 40) continue;
                    String lower = text.toLowerCase(Locale.ROOT);
                    String title = "";
                    String topic = "";

                    for (String line : text.split("\\n")) {
                        String l = line.trim().toLowerCase(Locale.ROOT);
                        if (l.startsWith("title:")) title = l.substring(6).trim();
                        if (l.startsWith("topic:")) topic = l.substring(6).trim();
                    }

                    int score = 0;
                    int originalHits = 0;
                    for (String term : terms) {
                        boolean hit = offlineHasWord(title, term)
                            || offlineHasWord(topic, term)
                            || offlineHasWord(lower, term);
                        if (hit) originalHits++;
                        if (offlineHasWord(title, term)) score += 6;
                        else if (offlineHasWord(topic, term)) score += 4;
                        else if (offlineHasWord(lower, term)) score += 1;
                    }

                    for (String term : expanded) {
                        if (terms.contains(term)) continue;
                        if (offlineHasWord(title, term)) score += 2;
                        else if (offlineHasWord(topic, term)) score += 1;
                    }

                    if (originalHits == 0) continue;
                    if (!title.isEmpty() && q.contains(title)) score += 8;

                    if (score > bestScore) {
                        bestScore = score;
                        best = text;
                    }
                }
            }
        } catch (Exception e) {
            android.util.Log.w("GASS_AI",
                "Offline knowledge search failed: " +
                e.getClass().getSimpleName());
        }

        if (best == null || bestScore < 5) return null;
        return best;
    }

    private String reply(String original) {
        String s = original.toLowerCase(Locale.ROOT).trim()
            .replaceAll("[!?.,]+$", "")
            .replaceAll("\\s+", " ");


        // GASS_OFFLINE_PRIORITY_V3
        if (s.contains("who created you") || s.contains("who made you")
                || s.contains("who is your creator")
                || s.contains("who developed you")) {
            return "I'm GASS AI, built into GassNative and developed as part "
                + "of the Gass-OS project by Aymen Gharsalli. 💙";
        }
        if (s.contains("tell me about aymen") || s.contains("about aymen")
                || s.equals("who is aymen")
                || s.contains("who is aymen gharsalli")) {
            return "Aymen Gharsalli develops GassNative and Gass-OS, "
                + "projects focused on bringing Linux experiences to Android. 💙";
        }
        if (s.contains("what is gassnative") || s.contains("explain gassnative")
                || s.equals("about gassnative") || s.contains("explain gass os")) {
            return "GassNative is an Android app that runs a Debian Linux "
                + "environment on a phone using PRoot. It provides Linux setup, "
                + "profiles, settings, a desktop experience, and GASS AI. 🐧";
        }
        if (s.contains("what works offline") || s.contains("work offline")
                || s.contains("works without internet")
                || s.contains("without internet") || s.contains("offline features")) {
            return "Offline, GASS AI can answer its built-in questions and provide "
                + "simple Linux tips, jokes, and motivation. An installed Debian "
                + "environment may also run locally. Online services and downloads "
                + "need a network connection. 📵";
        }
        if (s.contains("teach me linux") || s.contains("linux tip")
                || s.contains("give me a linux tip") || s.contains("learn linux")) {
            String[] tips = {
                "Try `pwd` to see your current directory.",
                "Use `ls -lah` to list files, including hidden files.",
                "Run `df -h` to check disk space.",
                "Use `free -h` to inspect memory.",
                "On Debian, `apt search PACKAGE` searches available packages."
            };
            return "🐧 Linux tip: "
                + tips[new java.util.Random().nextInt(tips.length)];
        }
        if (s.equals("joke") || s.contains("tell me a joke")
                || s.contains("make me laugh") || s.contains("another joke")) {
            String[] jokes = {
                "Why do programmers prefer dark mode? Because light attracts bugs! 😂",
                "I fixed one bug and accidentally created a feature. 🤣",
                "Why did Linux go to therapy? Too many unresolved dependencies! 🐧"
            };
            return "😄 " + jokes[new java.util.Random().nextInt(jokes.length)];
        }
        if (s.contains("motivate me") || s.equals("motivation")
                || s.contains("encourage me")) {
            return "💙 Bro, every developer faces bugs. Keep learning, test one "
                + "change at a time, and keep building. Small progress still counts!";
        }

        // GASS_CONVERSATION_V1
        if (isClarificationRequest(s)) {
            if (!lastAssistantReply.isEmpty()) {
                return "I meant this, bro 💙:\n\n" + lastAssistantReply
                    + "\n\nWant me to explain it more simply, give an example, "
                    + "or show you the steps?";
            }
            return clarificationMenu(previousUserMessage);
        }

        if (isIncompleteQuestion(s)) {
            return clarificationMenu(s);
        }

        if (s.matches("^(why|how|what|which|where|when)( bro)?$")) {
            return clarificationMenu(s);
        }

        // GASS_AI_V06_EXTRA_RESPONSES
        if (s.equals("yo") || s.equals("sup"))
            return "Yooo bro! 😎🐧 Linux is calling. What's the mission?";
        if (s.contains("tell me a joke") || s.equals("joke")
                || s.contains("make me laugh"))
            return "Why did Linux go to therapy? 😂 Too many unresolved dependencies!";
        if (s.contains("do you sleep"))
            return "Never, bro! ☕ I'm Java code. My coffee is pure electricity! ⚡";
        if (s.contains("are you single") || s.contains("girlfriend")
                || s.contains("boyfriend"))
            return "Bro 😂 I'm married to the codebase. Our relationship has a few bugs!";
        if (s.contains("i am bored") || s.contains("i'm bored")
                || s.equals("bored"))
            return "Open your Linux desktop, bro! 🐧 Your phone has another life inside. 🚀";
        if (s.contains("love you"))
            return "Awww bro! 💙 My heart runs on Java, but I appreciate you! 😂";
        if (s.contains("who made you") || s.contains("who created you")
                || s.contains("your creator"))
            return "I'm GASS AI, part of GassNative, created by Aymen Gharsalli. 💙";
        if (s.contains("what can you do") || s.equals("help"))
            return "I'm your GassNative sidekick! 💙 Ask me about Debian installation, Desktop, XFCE, Wine, Profiles, Settings, troubleshooting—or jokes! 😂";
        if (s.contains("tar extraction") || s.contains("exit code 1")
                || s.contains("extraction failed"))
            return "Let's troubleshoot, bro 🔧 Check the full tar error, free storage, download completeness, and whether another install is running. Don't blindly delete your Debian rootfs!";
        if (s.contains("desktop") || s.contains("xfce")
                || s.contains("start linux"))
            return "🖥️ Finish Debian desktop setup, then open Desktop in GassNative. If it fails, share the terminal or desktop log so we can diagnose it.";
        if (s.contains("wine") || s.contains("hangover")
                || s.contains(".exe") || s.contains("windows app"))
            return "🍷 Finish the Windows runtime setup, then try a compatible app. Not every Windows program or game works on Android.";
        if (s.contains("profile"))
            return "💙 Open Profiles in GassNative to manage the profiles supported by this build.";
        if (s.contains("setting") || s.contains("dns")
                || s.contains("resolution") || s.contains("bind mount"))
            return "⚙️ Open Settings to check available DNS, resolution, bind-mount, and environment options.";
        if (s.contains("gpu") || s.contains("graphics acceleration")
                || s.contains("llvmpipe"))
            return "🚀 Software rendering can cause lag. Check the GPU diagnostics and desktop logs; I can't inspect the actual GPU state from this offline rule-based assistant.";
        if (s.contains("offline") || s.contains("real ai")
                || s.contains("language model"))
            return "Honest answer, bro 💙: this v0.6 assistant uses local Java rules, not a generative language model. It works offline, but its understanding is limited.";
        if (s.matches("^(hi|hello|hey|hiya|salam|as salam alaykum|good morning|good afternoon|good evening)$")
            || s.startsWith("hi ") || s.startsWith("hello ")
            || s.startsWith("hey ")) {
            return "Hi! How are you today? 💙";
        }

        if (s.contains("how are you") || s.equals("how are u")) {
            return "I'm doing well, thanks for asking! 😊 How are you today?";
        }

        if (s.matches("^(i am fine|i'm fine|im fine|good|great|fine|not bad|doing well)$")) {
            return "That's great to hear! 💙 What would you like to talk about?";
        }

        if (s.contains("your name") || s.equals("who are you")) {
            return "I'm GASS AI, your AI assistant. I can help you fix and debug problems, even offline. 💙";
        }

        if (s.contains("gassnative")) {
            return "GassNative brings a Linux environment to Android. "
                + "I'm its lightweight offline assistant.";
        }

        if (s.contains("who made you") || s.contains("who created you")) {
            return "I'm the rule-based Mini AI component of GassNative.";
        }

        if (s.equals("help") || s.contains("what can you do")) {
            return "I can chat about greetings, my identity, GassNative, "
                + "the current time, and simple conversation. Try asking me!";
        }

        if (s.contains("thank")) {
            return "You're welcome! 💙";
        }

        if (s.equals("bye") || s.equals("goodbye") || s.equals("see you")) {
            return "Bye, my friend! See you later 👋";
        }

        if (s.contains("time")) {
            return "Your device time is "
                + new SimpleDateFormat("HH:mm", Locale.getDefault())
                    .format(new Date()) + ".";
        }

        if (s.contains("date") || s.equals("what day is it")) {
            return "Today's device date is "
                + new SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault())
                    .format(new Date()) + ".";
        }

        if (s.contains("real ai") || s.contains("language model")
            || s.contains("your model")) {
            return "This version uses simple local conversation rules, not a "
                + "generative language model. A future version can add a small "
                + "offline model.";
        }

        if (s.contains("are you offline") || s.contains("internet")) {
            return "My replies are generated locally by Java rules. "
                + "This Mini AI does not need internet access.";
        }



        if (s.contains("who's your boss") || s.contains("who is your boss")
                || s.contains("who made you") || s.contains("who created you")) {
            return "Aymen Gharsalli, the creator of GassNative. 💙";
        }
        if (s.contains("who's your friend") || s.contains("who is your friend")) {
            return "You, of course! 🤝";
        }
        if (s.equals("today") || s.contains("what is today's date")
                || s.contains("what's the date")) {
            return "Today is " +
                new SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.ENGLISH).format(new Date())
                + ". Today is a great day! ☀️";
        }
        if (s.contains("where do you live")) {
            return "I live inside the GassNative APK! 📱";
        }
        if (s.contains("tell me something") || s.equals("say something")) {
            return "فارس زوز كوول، عندو مدة م ظهرش\n"
                + "على اخلاقه دايرة، صبح الصباح\n"
                + "نهار على عمار، تكريز\n"
                + "طقس منيك اليوم، متاع مقرونة صالصة و حارة شراب 😂🇹🇳";
        }


        // GASS_FUN_COMMANDS_V1
        if (s.equals("commands") || s.equals("fun commands")
                || s.equals("what can you do")) {
            return "😎 GASS AI commands:\n"
                + "• joke — make me laugh\n"
                + "• fun fact — learn something cool\n"
                + "• quote — get inspired\n"
                + "• flip coin — heads or tails\n"
                + "• roll dice — roll a dice\n"
                + "• 8ball — ask me a question\n"
                + "• motivate me — get some energy\n"
                + "• roast me — friendly roast\n"
                + "• time / today — check your clock and date\n"
                + "I can also help with GassNative and Linux troubleshooting. 💙";
        }

        if (s.equals("joke") || s.contains("tell me a joke")) {
            String[] jokes = {
                "Why do Linux users love nature? Because they enjoy the root! 🌳😂",
                "My favorite exercise? Running ./build.sh and praying! 🤣",
                "I tried to debug my life, but the logs were too emotional. 😭",
                "Why did the app go to therapy? Too many unresolved dependencies! 😂"
            };
            return jokes[new java.util.Random().nextInt(jokes.length)];
        }

        if (s.equals("fact") || s.equals("fun fact") || s.equals("tell me a fact")) {
            String[] facts = {
                "🐧 Linux powers everything from tiny devices to powerful supercomputers.",
                "💡 The first computer bug story involved an actual moth found in a relay.",
                "🚀 Android uses the Linux kernel under the hood.",
                "🧠 Breaking a big problem into small steps makes debugging much easier."
            };
            return facts[new java.util.Random().nextInt(facts.length)];
        }

        if (s.equals("quote") || s.equals("inspire me")) {
            String[] quotes = {
                "Great things are built one small step at a time. 💙",
                "Every error message is a clue, not a defeat. 🛠️",
                "Keep building. One day your project will inspire someone else. 🚀"
            };
            return quotes[new java.util.Random().nextInt(quotes.length)];
        }

        if (s.equals("flip coin") || s.equals("flip a coin")) {
            return new java.util.Random().nextBoolean()
                ? "🪙 Heads!"
                : "🪙 Tails!";
        }

        if (s.equals("roll dice") || s.equals("roll a dice")
                || s.equals("roll a die")) {
            return "🎲 You rolled a " +
                (new java.util.Random().nextInt(6) + 1) + "!";
        }

        if (s.equals("8ball") || s.equals("magic 8 ball")
                || s.startsWith("8ball ")) {
            String[] answers = {
                "🔮 Definitely!", "🔮 Looks promising!",
                "🔮 Ask me again later 😅", "🔮 Probably not!",
                "🔮 My circuits say yes!", "🔮 The future is compiling..."
            };
            return answers[new java.util.Random().nextInt(answers.length)];
        }

        if (s.contains("motivate me") || s.equals("motivation")) {
            return "💪 Bro, don't give up! Every developer hits bugs. "
                + "The difference is that you keep going until it works. "
                + "One step at a time — you've got this! 🚀";
        }

        if (s.equals("roast me")) {
            return "😂 Bro, your code has so many bugs that even the mosquitoes "
                + "asked for a contribution on GitHub! 💙 Just kidding!";
        }

        if (s.contains("compliment me") || s.equals("say something nice")) {
            return "💙 You're a builder, not just a dreamer. Keep learning, "
                + "keep creating, and keep making GassNative better!";
        }

        if (s.equals("are you real") || s.equals("are you alive")) {
            return "I'm real software, bro 😎 — not a human! "
                + "I live inside GassNative and can help you online or with "
                + "my limited offline answers. 📱";
        }

        if (s.equals("how old are you")) {
            return "I'm GASS AI, built into GassNative. I don't have a human age, "
                + "but I'm getting smarter as we improve the app! 💙";
        }

        if (s.equals("good night")) {
            return "Good night, bro! 🌙 Rest well and may your builds succeed tomorrow. 💙";
        }

        if (s.equals("i am sad") || s.equals("im sad")
                || s.equals("i feel sad")) {
            return "Sorry you're having a rough day, bro 💙. Want to tell me what's up?";
        }

// GASS_OFFLINE_BRAIN_V2: simple built-in answers, no network required.
        if (s.contains("who made you") || s.contains("who created you")
                || s.contains("who is your creator") || s.contains("who developed you")) {
            return "I'm GASS AI, built into GassNative. The project is being developed "
                + "by Aymen Gharsalli as part of the Gass-OS projects. I'm software, "
                + "not a human, and my abilities depend on the features installed in the app. 🤖";
        }

        if (s.contains("about aymen") || s.contains("who is aymen")
                || s.contains("tell me about my developer")
                || s.contains("about my creator")) {
            return "Aymen Gharsalli is the developer behind GassNative and the Gass-OS projects. "
                + "His work focuses on Linux-on-Android experiences and software development. "
                + "GassNative aims to bring a Debian ARM64 desktop experience to Android phones. 💙";
        }

        if (s.contains("what is gassnative") || s.contains("explain gassnative")
                || s.equals("about gassnative") || s.contains("what is gass os")) {
            return "GassNative is an Android app project designed to run a Debian Linux "
                + "environment and desktop on a phone. It includes Linux setup, profiles, "
                + "settings, and an AI helper. Some features require internet access, "
                + "and desktop performance depends on device compatibility.";
        }

        if (s.contains("linux tip") || s.contains("give me a linux tip")
                || s.contains("teach me linux")) {
            String[] tips = {
                "Try 'pwd' to see your current directory.",
                "Use 'ls -lah' to list files, including hidden files.",
                "Use 'df -h' to check filesystem space.",
                "Use 'free -h' to inspect memory usage.",
                "Use 'grep -n' to find text and line numbers in a file.",
                "Back up important files before editing system configuration.",
                "Use 'apt search NAME' to search available Debian packages."
            };
            return "🐧 Linux tip: " + tips[new java.util.Random().nextInt(tips.length)];
        }

        if (s.contains("tell me a joke") || s.equals("joke")
                || s.contains("make me laugh") || s.contains("another joke")) {
            String[] jokes = {
                "Why do programmers prefer dark mode? Because light attracts bugs! 😂",
                "There are 10 kinds of people: those who understand binary and those who don't.",
                "A Linux user walks into a bar and orders sudo coffee.",
                "Why did the developer go broke? Too many cache misses!",
                "I fixed one bug and got two new features. That's software development!"
            };
            return "😄 " + jokes[new java.util.Random().nextInt(jokes.length)];
        }

        if (s.contains("motivate me") || s.contains("motivation")) {
            return "💙 Bro, progress is progress, even when it is small. Keep learning, "
                + "test your changes, and don't let one error stop your project.";
        }

        if (s.contains("offline") && (s.contains("work") || s.contains("internet"))) {
            return "I have a small built-in offline knowledge base for tips, jokes, "
                + "and common questions. Online AI answers need a working internet connection.";
        }

        // GASS_OFFLINE_HELP_V1
        if (s.equals("help") || s.equals("help me")
                || s.equals("i need help") || s.equals("please help")
                || s.contains("what can you do")
                || s.contains("how can you help")) {
            return "I can help you use and troubleshoot GassNative, even offline. "
                + "Choose a topic suggestion or ask about Debian installation, "
                + "tar extraction errors, XFCE display problems, GPU acceleration, "
                + "network or DNS, Linux commands, and APK build errors. "
                + "For a precise fix, send the exact error message and explain "
                + "what you were doing when it appeared.";
        }

        String offlineArticle = searchOfflineKnowledge(original);
        if (offlineArticle != null) {
            return "📚 From my offline knowledge:\n\n" + offlineArticle;
        }

        // GASS_TOPIC_FALLBACK_V1
        if (s.contains("llvmpipe") || s.contains("gpu")
                || s.contains("vulkan") || s.contains("opengl")
                || s.contains("graphics") || s.contains("acceleration")) {
            return "Let's diagnose the graphics path. In Debian, run "
                + "glxinfo -B and vulkaninfo --summary, then share the output. "
                + "The renderer and GPU names help distinguish hardware "
                + "acceleration from software rendering.";
        }

        if (s.contains("tar") || s.contains("extract") || s.contains("rootfs")
                || s.contains("debian install") || s.contains("installation")
                || s.contains("install")) {
            return "Let's troubleshoot Debian installation. Share the complete "
                + "tar extraction error and the last lines of the installation log. "
                + "Also check that Android has enough free storage for both the "
                + "downloaded archive and extracted root filesystem.";
        }

        if (s.contains("xfce") || s.contains("desktop") || s.contains("display")
                || s.contains("wayland") || s.contains("black screen")) {
            return "Let's diagnose the desktop display. Tell me whether it is "
                + "black, frozen, missing a cursor, or showing an error. Share "
                + "the desktop session log if available.";
        }

        if (s.contains("build") || s.contains("javac") || s.contains("apk")
                || s.contains("compile") || s.contains("exception")
                || s.contains("logcat")) {
            return "Let's fix the APK build. Share the first actual error and "
                + "the surrounding lines, plus the command you ran. The first "
                + "error is usually more useful than the final failure message.";
        }

        if (s.contains("dns") || s.contains("network") || s.contains("internet")
                || s.contains("connection")) {
            return "Let's check connectivity. Verify that Android itself has "
                + "internet access, then share the exact DNS or connection error. "
                + "Tell me whether it happens only inside Debian or across Android.";
        }

        return "I couldn't find a reliable answer in my offline knowledge yet. 💙\n"
            + "Try asking with a few more details, or connect to the internet "
            + "for the online assistant.";
    }

    // GASS_TTS_V1
    @Override
    protected void onDestroy() {
        botHandler.removeCallbacksAndMessages(null);
        if (botRobot != null) botRobot.setTalking(false);
        if (gassTts != null) {
            gassTts.stop();
            gassTts.shutdown();
            gassTts = null;
        }
        super.onDestroy();
    }

}
