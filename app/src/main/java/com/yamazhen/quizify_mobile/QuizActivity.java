package com.yamazhen.quizify_mobile;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class QuizActivity extends AppCompatActivity {
    private LinearLayout questionsContainer; // A layout in activity_quiz.xml where you will add your questions
    private Button submitButton;
    private JSONArray questionsArray; // Store the fetched questions

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz);

        questionsContainer = findViewById(R.id.questions_container);
        submitButton = findViewById(R.id.submit_button);

        String fileName = getIntent().getStringExtra("fileName");
        loadQuestions(fileName);

        submitButton.setOnClickListener(v -> submitAnswers());
    }

    private void loadQuestions(String fileName) {
        String url = "http://10.0.2.2:3000/questions/" + fileName;

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(url).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                e.printStackTrace();
                runOnUiThread(() ->
                        Toast.makeText(QuizActivity.this, "Failed to load questions", Toast.LENGTH_SHORT).show()
                );
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                if (!response.isSuccessful()) {
                    runOnUiThread(() ->
                            Toast.makeText(QuizActivity.this, "Server error: " + response.code(), Toast.LENGTH_SHORT).show()
                    );
                    return;
                }

                String responseBody = response.body().string();
                try {
                    JSONObject json = new JSONObject(responseBody);
                    questionsArray = json.getJSONArray("questions");

                    runOnUiThread(() -> displayQuestions(questionsArray));
                } catch (JSONException e) {
                    e.printStackTrace();
                    runOnUiThread(() ->
                            Toast.makeText(QuizActivity.this, "JSON parse error", Toast.LENGTH_SHORT).show()
                    );
                }
            }
        });
    }

    private void displayQuestions(JSONArray questions) {
        for (int i = 0; i < questions.length(); i++) {
            try {
                JSONObject q = questions.getJSONObject(i);
                int questionId = q.getInt("id");
                String questionText = q.getString("questionText");
                JSONArray choices = new JSONArray(q.getString("choices"));

                // Create Views for a single question:
                TextView questionTextView = new TextView(this);
                questionTextView.setText((i+1) + ". " + questionText);
                questionTextView.setTextColor(Color.BLACK);

                // A RadioGroup for multiple-choice
                RadioGroup radioGroup = new RadioGroup(this);
                radioGroup.setId(View.generateViewId());
                // Tag the questionId so we can retrieve it later
                radioGroup.setTag(questionId);

                // Add RadioButtons for each choice
                for (int j = 0; j < choices.length(); j++) {
                    String choice = choices.getString(j);
                    RadioButton rb = new RadioButton(this);
                    rb.setText(choice);
                    radioGroup.addView(rb);
                }

                // Add them to the container
                questionsContainer.addView(questionTextView);
                questionsContainer.addView(radioGroup);

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private void submitAnswers() {
        // Build the userAnswers array
        JSONArray userAnswers = new JSONArray();
        for (int i = 0; i < questionsContainer.getChildCount(); i++) {
            View v = questionsContainer.getChildAt(i);
            if (v instanceof RadioGroup) {
                RadioGroup group = (RadioGroup) v;
                int questionId = (int) group.getTag();
                int selectedId = group.getCheckedRadioButtonId();
                String answer = null;
                if (selectedId != -1) {
                    RadioButton selected = group.findViewById(selectedId);
                    answer = selected.getText().toString();
                }
                JSONObject ans = new JSONObject();
                try {
                    ans.put("questionId", questionId);
                    ans.put("answer", answer);
                    userAnswers.put(ans);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }

        // Send POST request to submit-answers
        String url = "http://10.0.2.2:3000/submit-answers";
        OkHttpClient client = new OkHttpClient();
        RequestBody body = RequestBody.create(
                MediaType.parse("application/json"),
                userAnswersWrapper(userAnswers).toString()
        );

        Request request = new Request.Builder().url(url).post(body).build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                e.printStackTrace();
                runOnUiThread(() ->
                        Toast.makeText(QuizActivity.this, "Failed to submit answers", Toast.LENGTH_SHORT).show()
                );
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                if(!response.isSuccessful()){
                    runOnUiThread(() ->
                            Toast.makeText(QuizActivity.this, "Server error: " + response.code(), Toast.LENGTH_SHORT).show()
                    );
                    return;
                }

                String responseBody = response.body().string();
                try{
                    JSONObject json = new JSONObject(responseBody);
                    JSONArray results = json.getJSONArray("results");
                    int score = json.getInt("score");
                    int totalQuestions = json.getInt("totalQuestions");

                    runOnUiThread(() -> showResults(results));
                } catch (JSONException e){
                    e.printStackTrace();
                    runOnUiThread(() ->
                            Toast.makeText(QuizActivity.this, "JSON parse error", Toast.LENGTH_SHORT).show()
                    );
                }
            }
        });
    }

    private JSONObject userAnswersWrapper(JSONArray userAnswers) {
        // The endpoint expects { "answers": [ ... ] }
        JSONObject wrapper = new JSONObject();
        try {
            wrapper.put("answers", userAnswers);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return wrapper;
    }

    private void showResults(JSONArray results) {
        int resultIndex = 0;
        for (int i = 0; i < questionsContainer.getChildCount(); i++) {
            View v = questionsContainer.getChildAt(i);
            if (v instanceof RadioGroup) {
                if (resultIndex < results.length()) {
                    try {
                        JSONObject result = results.getJSONObject(resultIndex);
                        boolean isCorrect = result.getBoolean("isCorrect");
                        String correctAnswer = result.getString("correctAnswer");

                        TextView feedback = new TextView(this);
                        if (isCorrect) {
                            feedback.setText("Correct!");
                            feedback.setTextColor(getResources().getColor(android.R.color.holo_green_light, null));
                        } else {
                            feedback.setText("Incorrect. The correct answer is: " + correctAnswer);
                            feedback.setTextColor(getResources().getColor(android.R.color.holo_red_light, null));
                        }

                        // Insert feedback right after the RadioGroup
                        int radioGroupIndex = questionsContainer.indexOfChild(v);
                        questionsContainer.addView(feedback, radioGroupIndex + 1);

                        resultIndex++;
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                } else {
                    // No corresponding result for this question (if user didn't answer all)
                    // You can decide what to do here if needed
                }
            }
        }
    }
}
