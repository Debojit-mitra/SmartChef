package com.bunny.ml.smartchef.models;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Recipe implements Parcelable {

    private String recipeId;
    private String name;
    private String cookTime;
    private String prepTime;
    private String totalTime;
    private String imageUrl;
    private List<String> ingredientParts;
    private List<String> ingredientQuantities;
    private Map<String, Double> nutritionalInfo;
    private List<InstructionStep> recipeInstructions;
    private List<String> equipmentNeeded;
    private double aggregatedRating;
    private int ratingCount;
    private int recipeServings;
    private double similarityScore;

    // Constructor
    public Recipe(String recipeId, String name, String cookTime, String prepTime,
                  String imageUrl, List<String> ingredientParts, List<String> ingredientQuantities,
                  Map<String, Double> nutritionalInfo, List<InstructionStep> recipeInstructions,
                  List<String> equipmentNeeded, double aggregatedRating, int ratingCount,
                  int recipeServings, double similarityScore) {
        this.recipeId = recipeId;
        this.name = name;
        this.cookTime = cookTime;
        this.prepTime = prepTime;
        this.totalTime = calculateTotalTime(cookTime, prepTime);
        this.imageUrl = imageUrl;
        this.ingredientParts = ingredientParts;
        this.ingredientQuantities = ingredientQuantities;
        this.nutritionalInfo = nutritionalInfo;
        this.recipeInstructions = recipeInstructions;
        this.equipmentNeeded = equipmentNeeded;
        this.aggregatedRating = aggregatedRating;
        this.ratingCount = ratingCount;
        this.recipeServings = recipeServings;
        this.similarityScore = similarityScore;
    }

    public String getRecipeId() {
        return recipeId;
    }

    public void setRecipeId(String recipeId) {
        this.recipeId = recipeId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCookTime() {
        return cookTime;
    }

    public void setCookTime(String cookTime) {
        this.cookTime = cookTime;
    }

    public String getPrepTime() {
        return prepTime;
    }

    public void setPrepTime(String prepTime) {
        this.prepTime = prepTime;
    }

    public String getTotalTime() {
        return totalTime;
    }

    public void setTotalTime(String totalTime) {
        this.totalTime = totalTime;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public List<String> getIngredientParts() {
        return ingredientParts;
    }

    public void setIngredientParts(List<String> ingredientParts) {
        this.ingredientParts = ingredientParts;
    }

    public List<String> getIngredientQuantities() {
        return ingredientQuantities;
    }

    public void setIngredientQuantities(List<String> ingredientQuantities) {
        this.ingredientQuantities = ingredientQuantities;
    }

    public Map<String, Double> getNutritionalInfo() {
        return nutritionalInfo;
    }

    public void setNutritionalInfo(Map<String, Double> nutritionalInfo) {
        this.nutritionalInfo = nutritionalInfo;
    }

    public List<InstructionStep> getRecipeInstructions() {
        return recipeInstructions;
    }

    public void setRecipeInstructions(List<InstructionStep> recipeInstructions) {
        this.recipeInstructions = recipeInstructions;
    }

    public List<String> getEquipmentNeeded() {
        return equipmentNeeded;
    }

    public void setEquipmentNeeded(List<String> equipmentNeeded) {
        this.equipmentNeeded = equipmentNeeded;
    }

    public double getAggregatedRating() {
        return aggregatedRating;
    }

    public void setAggregatedRating(double aggregatedRating) {
        this.aggregatedRating = aggregatedRating;
    }

    public int getRatingCount() {
        return ratingCount;
    }

    public void setRatingCount(int ratingCount) {
        this.ratingCount = ratingCount;
    }

    public int getRecipeServings() {
        return recipeServings;
    }

    public void setRecipeServings(int recipeServings) {
        this.recipeServings = recipeServings;
    }

    public double getSimilarityScore() {
        return similarityScore;
    }

    public void setSimilarityScore(double similarityScore) {
        this.similarityScore = similarityScore;
    }

    // Parcelable constructor
    protected Recipe(Parcel in) {
        recipeId = in.readString();
        name = in.readString();
        cookTime = in.readString();
        prepTime = in.readString();
        totalTime = in.readString();
        imageUrl = in.readString();
        ingredientParts = in.createStringArrayList();
        ingredientQuantities = in.createStringArrayList();

        // Read nutritionalInfo map
        int nutritionalInfoSize = in.readInt();
        nutritionalInfo = new HashMap<>();
        for (int i = 0; i < nutritionalInfoSize; i++) {
            String key = in.readString();
            double value = in.readDouble();
            nutritionalInfo.put(key, value);
        }

        // Read recipeInstructions
        int instructionsSize = in.readInt();
        recipeInstructions = new ArrayList<>();
        for (int i = 0; i < instructionsSize; i++) {
            recipeInstructions.add(in.readParcelable(InstructionStep.class.getClassLoader()));
        }

        equipmentNeeded = in.createStringArrayList();
        aggregatedRating = in.readDouble();
        ratingCount = in.readInt();
        recipeServings = in.readInt();
        similarityScore = in.readDouble();
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeString(recipeId);
        dest.writeString(name);
        dest.writeString(cookTime);
        dest.writeString(prepTime);
        dest.writeString(totalTime);
        dest.writeString(imageUrl);
        dest.writeStringList(ingredientParts);
        dest.writeStringList(ingredientQuantities);

        // Write nutritionalInfo map
        dest.writeInt(nutritionalInfo.size());
        for (Map.Entry<String, Double> entry : nutritionalInfo.entrySet()) {
            dest.writeString(entry.getKey());
            dest.writeDouble(entry.getValue());
        }

        // Write recipeInstructions
        dest.writeInt(recipeInstructions.size());
        for (InstructionStep step : recipeInstructions) {
            dest.writeParcelable(step, flags);
        }

        dest.writeStringList(equipmentNeeded);
        dest.writeDouble(aggregatedRating);
        dest.writeInt(ratingCount);
        dest.writeInt(recipeServings);
        dest.writeDouble(similarityScore);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<Recipe> CREATOR = new Creator<Recipe>() {
        @Override
        public Recipe createFromParcel(Parcel in) {
            return new Recipe(in);
        }

        @Override
        public Recipe[] newArray(int size) {
            return new Recipe[size];
        }
    };


    // Nested class for structured instructions
    public static class InstructionStep implements Parcelable {
        private int stepNumber;
        private String text;
        private List<String> actions;
        private List<String> ingredientsMentioned;
        private Map<String, Integer> timing;
        private boolean requiresAttention;

        public InstructionStep(int stepNumber, String text, List<String> actions,
                               List<String> ingredientsMentioned, Map<String, Integer> timing,
                               boolean requiresAttention) {
            this.stepNumber = stepNumber;
            this.text = text;
            this.actions = actions;
            this.ingredientsMentioned = ingredientsMentioned;
            this.timing = timing;
            this.requiresAttention = requiresAttention;
        }

        protected InstructionStep(Parcel in) {
            stepNumber = in.readInt();
            text = in.readString();
            actions = in.createStringArrayList();
            ingredientsMentioned = in.createStringArrayList();

            // Read timing map
            int timingSize = in.readInt();
            timing = new HashMap<>();
            for (int i = 0; i < timingSize; i++) {
                String key = in.readString();
                int value = in.readInt();
                timing.put(key, value);
            }

            requiresAttention = in.readByte() != 0;
        }

        @Override
        public void writeToParcel(@NonNull Parcel dest, int flags) {
            dest.writeInt(stepNumber);
            dest.writeString(text);
            dest.writeStringList(actions);
            dest.writeStringList(ingredientsMentioned);

            // Write timing map
            dest.writeInt(timing.size());
            for (Map.Entry<String, Integer> entry : timing.entrySet()) {
                dest.writeString(entry.getKey());
                dest.writeInt(entry.getValue());
            }

            dest.writeByte((byte) (requiresAttention ? 1 : 0));
        }

        @Override
        public int describeContents() {
            return 0;
        }

        public static final Creator<InstructionStep> CREATOR = new Creator<InstructionStep>() {
            @Override
            public InstructionStep createFromParcel(Parcel in) {
                return new InstructionStep(in);
            }

            @Override
            public InstructionStep[] newArray(int size) {
                return new InstructionStep[size];
            }
        };

        public int getStepNumber() {
            return stepNumber;
        }

        public void setStepNumber(int stepNumber) {
            this.stepNumber = stepNumber;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }

        public List<String> getActions() {
            return actions;
        }

        public void setActions(List<String> actions) {
            this.actions = actions;
        }

        public List<String> getIngredientsMentioned() {
            return ingredientsMentioned;
        }

        public void setIngredientsMentioned(List<String> ingredientsMentioned) {
            this.ingredientsMentioned = ingredientsMentioned;
        }

        public Map<String, Integer> getTiming() {
            return timing;
        }

        public void setTiming(Map<String, Integer> timing) {
            this.timing = timing;
        }

        public boolean isRequiresAttention() {
            return requiresAttention;
        }

        public void setRequiresAttention(boolean requiresAttention) {
            this.requiresAttention = requiresAttention;
        }
    }



    private String calculateTotalTime(String cookTime, String prepTime) {
        try {
            int cookMinutes = parseTimeToMinutes(cookTime);
            int prepMinutes = parseTimeToMinutes(prepTime);
            int totalMinutes = cookMinutes + prepMinutes;

            if (totalMinutes < 60) {
                return totalMinutes + "m";
            } else {
                int hours = totalMinutes / 60;
                int minutes = totalMinutes % 60;
                return hours + "h " + (minutes > 0 ? minutes + "m" : "");
            }
        } catch (Exception e) {
            return "N/A";
        }
    }

    private int parseTimeToMinutes(String time) {
        if (time == null) return 0;
        String[] parts = time.split("h |m");
        int minutes = 0;
        if (time.contains("h")) {
            minutes += Integer.parseInt(parts[0]) * 60;
            if (parts.length > 1 && !parts[1].isEmpty()) {
                minutes += Integer.parseInt(parts[1]);
            }
        } else {
            minutes += Integer.parseInt(parts[0]);
        }
        return minutes;
    }
}
