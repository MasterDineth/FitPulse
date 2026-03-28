package com.mastersoft.fitpulse.data;

import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mastersoft.fitpulse.model.Tutorial;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class TutorialManager {

    private static final String FILE_NAME = "tutorials_data.json";
    private static TutorialManager instance;
    private List<Tutorial> allTutorials;

    private TutorialManager() {
        allTutorials = new ArrayList<>();
    }

    public static synchronized TutorialManager getInstance() {
        if (instance == null) {
            instance = new TutorialManager();
        }
        return instance;
    }

    public void initialize(Context context) {
        initLocalDataFile(context);
        loadLocalData(context);
    }

    public List<Tutorial> getAllTutorials() {
        return new ArrayList<>(allTutorials);
    }


    public List<Tutorial> getTutorialsByLevel(String level) {
        List<Tutorial> filtered = new ArrayList<>();
        for (Tutorial t : allTutorials) {
            if (t.getLevel().equalsIgnoreCase(level)) {
                filtered.add(t);
            }
        }
        return filtered;
    }

    private void initLocalDataFile(Context context) {
        File file = new File(context.getFilesDir(), FILE_NAME);
        if (!file.exists()) {
            List<Tutorial> defaultList = new ArrayList<>();
            // Strength
            defaultList.add(new Tutorial("Perfect Push-Up", "Beginner", "5 min", "💪", "https://www.youtube.com/results?search_query=perfect+push+up+form+tutorial"));
            defaultList.add(new Tutorial("Deadlift Masterclass", "Advanced", "15 min", "🏋️", "https://www.youtube.com/results?search_query=deadlift+masterclass+tutorial"));
            defaultList.add(new Tutorial("Squat Variations", "Beginner", "10 min", "🦵", "https://www.youtube.com/results?search_query=squat+variations+tutorial"));
            defaultList.add(new Tutorial("Bench Press Basics", "Intermediate", "10 min", "🏋️", "https://www.youtube.com/results?search_query=bench+press+basics+tutorial"));
            defaultList.add(new Tutorial("Pull-Up Progression", "Intermediate", "8 min", "💪", "https://www.youtube.com/results?search_query=pull+up+progression+tutorial"));
            defaultList.add(new Tutorial("Bicep Curls Form", "Beginner", "5 min", "💪", "https://www.youtube.com/results?search_query=bicep+curls+form+tutorial"));
            defaultList.add(new Tutorial("Tricep Extensions", "Beginner", "6 min", "💪", "https://www.youtube.com/results?search_query=tricep+extensions+tutorial"));
            defaultList.add(new Tutorial("Overhead Press", "Intermediate", "10 min", "🏋️", "https://www.youtube.com/results?search_query=overhead+press+tutorial"));
            defaultList.add(new Tutorial("Chest Dips", "Intermediate", "8 min", "💪", "https://www.youtube.com/results?search_query=chest+dips+tutorial"));
            defaultList.add(new Tutorial("Advanced Lunges", "Advanced", "12 min", "🦵", "https://www.youtube.com/results?search_query=advanced+lunges+tutorial"));
            // Cardio / HIIT
            defaultList.add(new Tutorial("Cardio HIIT Circuit", "Intermediate", "20 min", "🏃", "https://www.youtube.com/results?search_query=cardio+hiit+circuit+tutorial"));
            defaultList.add(new Tutorial("Burpee Challenge", "Advanced", "15 min", "🔥", "https://www.youtube.com/results?search_query=burpee+challenge+tutorial"));
            defaultList.add(new Tutorial("Jumping Jacks Cardio", "Beginner", "8 min", "🏃", "https://www.youtube.com/results?search_query=jumping+jacks+cardio+tutorial"));
            defaultList.add(new Tutorial("Mountain Climber HIIT", "Intermediate", "10 min", "🔥", "https://www.youtube.com/results?search_query=mountain+climber+hiit+tutorial"));
            defaultList.add(new Tutorial("Kettlebell Swings", "Intermediate", "12 min", "🏋️", "https://www.youtube.com/results?search_query=kettlebell+swings+tutorial"));
            defaultList.add(new Tutorial("Box Jumps Form", "Advanced", "10 min", "🏃", "https://www.youtube.com/results?search_query=box+jumps+form+tutorial"));
            defaultList.add(new Tutorial("High Knees Sprint", "Intermediate", "5 min", "🔥", "https://www.youtube.com/results?search_query=high+knees+sprint+tutorial"));
            defaultList.add(new Tutorial("Battle Ropes HIIT", "Advanced", "12 min", "🔥", "https://www.youtube.com/results?search_query=battle+ropes+hiit+tutorial"));
            defaultList.add(new Tutorial("Shadow Boxing", "All Levels", "15 min", "🥊", "https://www.youtube.com/results?search_query=shadow+boxing+tutorial"));
            defaultList.add(new Tutorial("Bounding Drills", "Advanced", "10 min", "🏃", "https://www.youtube.com/results?search_query=bounding+drills+tutorial"));
            // Core
            defaultList.add(new Tutorial("Core Strength 101", "Intermediate", "12 min", "🤸", "https://www.youtube.com/results?search_query=core+strength+101+tutorial"));
            defaultList.add(new Tutorial("Plank for Beginners", "Beginner", "5 min", "🧘", "https://www.youtube.com/results?search_query=plank+for+beginners+tutorial"));
            defaultList.add(new Tutorial("Russian Twists", "Intermediate", "6 min", "🤸", "https://www.youtube.com/results?search_query=russian+twists+tutorial"));
            defaultList.add(new Tutorial("Hanging Leg Raises", "Advanced", "8 min", "🤸", "https://www.youtube.com/results?search_query=hanging+leg+raises+tutorial"));
            defaultList.add(new Tutorial("Sit-ups Variations", "Beginner", "7 min", "🤸", "https://www.youtube.com/results?search_query=sit+ups+variations+tutorial"));
            defaultList.add(new Tutorial("Superman Back Extension", "Beginner", "6 min", "🤸", "https://www.youtube.com/results?search_query=superman+back+extension+tutorial"));
            // Mobility / Recovery
            defaultList.add(new Tutorial("Shoulder Mobility", "All Levels", "7 min", "🔄", "https://www.youtube.com/results?search_query=shoulder+mobility+tutorial"));
            defaultList.add(new Tutorial("Morning Yoga Flow", "All Levels", "20 min", "🧘", "https://www.youtube.com/results?search_query=morning+yoga+flow+tutorial"));
            defaultList.add(new Tutorial("Full Body Stretching", "All Levels", "15 min", "🔄", "https://www.youtube.com/results?search_query=full+body+stretching+tutorial"));
            defaultList.add(new Tutorial("Foam Rolling Guide", "Intermediate", "10 min", "🔄", "https://www.youtube.com/results?search_query=foam+rolling+guide+tutorial"));


            try (FileWriter writer = new FileWriter(file)) {
                new Gson().toJson(defaultList, writer);
            } catch (IOException e) {
                Log.e("TutorialManager", "Error writing default file", e);
            }
        }
    }

    private void loadLocalData(Context context) {
        File file = new File(context.getFilesDir(), FILE_NAME);
        if (file.exists()) {
            try (FileReader reader = new FileReader(file)) {
                Type type = new TypeToken<ArrayList<Tutorial>>() {
                }.getType();
                allTutorials = new Gson().fromJson(reader, type);
            } catch (IOException e) {
                Log.e("TutorialManager", "Error reading local file", e);
            }
        }
    }
}