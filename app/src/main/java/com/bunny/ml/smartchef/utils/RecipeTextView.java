package com.bunny.ml.smartchef.utils;


import android.content.Context;
import android.graphics.Typeface;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.BulletSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatTextView;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RecipeTextView extends AppCompatTextView {
    private static final Pattern HEADER_PATTERN = Pattern.compile("^\\*\\*(.*?):\\*\\*\\*?$", Pattern.MULTILINE);
    private static final Pattern BULLET_PATTERN = Pattern.compile("^\\* (.+)$", Pattern.MULTILINE);
    private static final Pattern NUMBERED_PATTERN = Pattern.compile("^(\\d+\\.) (.+)$", Pattern.MULTILINE);
    private static final Pattern NESTED_BULLET_PATTERN = Pattern.compile("^\\s+\\* (.+)$", Pattern.MULTILINE);
    private static final Pattern BOLD_PATTERN = Pattern.compile("\\*\\*(.*?)\\*\\*");
    private static final int BULLET_GAP_WIDTH = 30;
    private static final int NESTED_BULLET_GAP_WIDTH = 60;
    private static final float HEADER_TEXT_SIZE_MULTIPLIER = 1.2f;
    private static final String NUMBERED_POINT_SPACING = "\n\n";
    private static final String BULLET_POINT_SPACING = "\n";

    public RecipeTextView(Context context) {
        super(context);
    }

    public RecipeTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public RecipeTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }
    public void setFormattedText(String text) {
        SpannableStringBuilder builder = new SpannableStringBuilder();
        String[] lines = text.split("\n");
        int lastNumberedIndex = -1;

        // First pass: Process the text and add appropriate spacing
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];

            // Check if current line is a numbered point
            boolean isNumbered = line.matches("^\\d+\\..*$");
            boolean isNested = line.trim().startsWith("* ");

            // Add appropriate spacing before the line
            if (i > 0) {
                // If this is a numbered point and we had a previous numbered point
                if (isNumbered && lastNumberedIndex != -1) {
                    // Add extra spacing
                    builder.append(NUMBERED_POINT_SPACING);
                } else if (!isNested) {
                    builder.append(BULLET_POINT_SPACING);
                } else {
                    builder.append(BULLET_POINT_SPACING);
                }
            }

            builder.append(line);

            // Update last numbered point index
            if (isNumbered) {
                lastNumberedIndex = i;
            }
        }

        String processedText = builder.toString();
        builder = new SpannableStringBuilder(processedText);

        // Process headers
        Matcher headerMatcher = HEADER_PATTERN.matcher(builder);
        while (headerMatcher.find()) {
            int start = headerMatcher.start(1);
            int end = headerMatcher.end(1);
            String headerText = headerMatcher.group(1);

            int fullStart = headerMatcher.start();
            int fullEnd = headerMatcher.end();

            builder.replace(fullStart, fullEnd, headerText);

            builder.setSpan(new StyleSpan(Typeface.BOLD),
                    fullStart, fullStart + headerText.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            builder.setSpan(new RelativeSizeSpan(HEADER_TEXT_SIZE_MULTIPLIER),
                    fullStart, fullStart + headerText.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            headerMatcher = HEADER_PATTERN.matcher(builder);
        }

        // Process numbered points
        Matcher numberedMatcher = NUMBERED_PATTERN.matcher(builder);
        while (numberedMatcher.find()) {
            int numberStart = numberedMatcher.start(1);
            int numberEnd = numberedMatcher.end(1);

            builder.setSpan(new StyleSpan(Typeface.BOLD),
                    numberStart, numberEnd,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        // Process regular bullet points
        Matcher bulletMatcher = BULLET_PATTERN.matcher(builder);
        while (bulletMatcher.find()) {
            int start = bulletMatcher.start(1);
            int end = bulletMatcher.end(1);
            int lineStart = bulletMatcher.start();

            builder.delete(lineStart, start);
            end = end - 2;

            builder.setSpan(new BulletSpan(BULLET_GAP_WIDTH),
                    lineStart, end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            bulletMatcher = BULLET_PATTERN.matcher(builder);
        }

        // Process nested bullet points
        Matcher nestedBulletMatcher = NESTED_BULLET_PATTERN.matcher(builder);
        while (nestedBulletMatcher.find()) {
            int start = nestedBulletMatcher.start(1);
            int end = nestedBulletMatcher.end(1);
            int lineStart = nestedBulletMatcher.start();

            builder.delete(start - 2, start);

            builder.setSpan(new BulletSpan(NESTED_BULLET_GAP_WIDTH),
                    lineStart, end - 2,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            nestedBulletMatcher = NESTED_BULLET_PATTERN.matcher(builder);
        }

        // Process remaining bold text
        Matcher boldMatcher = BOLD_PATTERN.matcher(builder);
        while (boldMatcher.find()) {
            int start = boldMatcher.start(1);
            int end = boldMatcher.end(1);

            builder.delete(end, end + 2);
            builder.delete(start - 2, start);

            builder.setSpan(new StyleSpan(Typeface.BOLD),
                    start - 2, end - 2,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            boldMatcher = BOLD_PATTERN.matcher(builder);
        }

        setText(builder);
    }
}
