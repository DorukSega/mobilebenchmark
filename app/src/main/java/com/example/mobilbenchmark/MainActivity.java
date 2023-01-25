package com.example.mobilbenchmark;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.jetbrains.annotations.NotNull;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;


public class MainActivity extends AppCompatActivity {
    private LinearLayout linearLayout;
    private ConstraintLayout consRight, consLeft;
    private TextView stateText, progressText;
    private EditText durationInput;
    private ProgressBar progressBar;
    private Button button1,button2;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        linearLayout = findViewById(R.id.mylayout);
        consRight = findViewById(R.id.constraintR);
        consLeft = findViewById(R.id.constraintL);
        button1 = (Button) findViewById(R.id.button1);
        button2 = (Button) findViewById(R.id.button2);
        stateText = (TextView) findViewById(R.id.state_text);
        progressText = (TextView) findViewById(R.id.progress_text);
        durationInput = findViewById(R.id.duration_input);
        progressBar = findViewById(R.id.progressBar);

        button1.setOnClickListener(v -> {
            BenchmarkTask singlethreaded = new BenchmarkTask(this);
            singlethreaded.nThreads=1;
            if (!durationInput.getText().toString().equals("")){
                String dur = durationInput.getText().toString();
                singlethreaded.BENCHMARK_DURATION_SECONDS = Integer.parseInt(dur);
            }

            button1.setClickable(false);
            button2.setClickable(false);
            progressText.setText("0%");
            stateText.setText("Running a Single Threaded Test");
            progressBar.setProgress(0);
            singlethreaded.execute();
        });

        button2.setOnClickListener(v -> {
            BenchmarkTask multithreaded= new BenchmarkTask(this);
            multithreaded.nThreads=4;
            if (!durationInput.getText().toString().equals("")){
                String dur = durationInput.getText().toString();
                multithreaded.BENCHMARK_DURATION_SECONDS = Integer.parseInt(dur);
            }

            button1.setClickable(false);
            button2.setClickable(false);
            progressText.setText("0%");
            progressBar.setProgress(0);
            stateText.setText("Running a Multi Threaded Test");
            multithreaded.execute();
        });
    }

    @Override
    public void onConfigurationChanged(@NotNull Configuration newConfig) {
        // this is used for dynamic ui shift in conjuncture to orientation change
        super.onConfigurationChanged(newConfig);
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            consRight.getLayoutParams().width = ConstraintLayout.LayoutParams.WRAP_CONTENT;
            consRight.getLayoutParams().height = ConstraintLayout.LayoutParams.MATCH_PARENT;
            consLeft.getLayoutParams().width = ConstraintLayout.LayoutParams.WRAP_CONTENT;
            consLeft.getLayoutParams().height = ConstraintLayout.LayoutParams.MATCH_PARENT;
        } else if (newConfig.orientation  == Configuration.ORIENTATION_PORTRAIT){
            consRight.getLayoutParams().width = ConstraintLayout.LayoutParams.MATCH_PARENT;
            consRight.getLayoutParams().height = ConstraintLayout.LayoutParams.WRAP_CONTENT;
            consLeft.getLayoutParams().width = ConstraintLayout.LayoutParams.MATCH_PARENT;
            consLeft.getLayoutParams().height = ConstraintLayout.LayoutParams.WRAP_CONTENT;
        }
        linearLayout.setOrientation(newConfig.orientation);
    }


    private static class BenchmarkTask extends AsyncTask<Void, Integer, Long> {
        public int nThreads = 1;
        public int BENCHMARK_DURATION_SECONDS = 10;
        private Integer totIterations = 0;
        private int maxIterationDuration = 0;

        private final WeakReference<MainActivity> activityReference;

        BenchmarkTask(MainActivity context) {
            activityReference = new WeakReference<>(context);
        }

        @Override
        protected Long doInBackground(Void... voids) {
           // Start the CPU benchmark.
            long startTime = System.currentTimeMillis();
            long endTime = startTime + BENCHMARK_DURATION_SECONDS * 1000L;

            long totalTime = 0;
            int iterations = 0;
            while (System.currentTimeMillis() < endTime) {
                // Perform the CPU benchmark.
                long iterationStartTime = System.currentTimeMillis();
                performCpuBenchmark(nThreads);
                long iterationEndTime = System.currentTimeMillis();

                // Update the total time and number of iterations.
                long duration = iterationEndTime - iterationStartTime;
                totalTime += iterationEndTime - iterationStartTime;
                iterations++;

                // Update the progress in percentage.
                int progress = (int) (((iterationEndTime - startTime) * 100) / (endTime - startTime));
                publishProgress(Math.min(progress, 100),(int)duration);
            }
            totIterations=iterations;
            // Calculate and return the benchmark result.
            return  (iterations * 1000000L) / totalTime ;
        }


        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            MainActivity activity = activityReference.get();
            if (activity == null || activity.isFinishing()) return;
            TextView sprogressText = activity.findViewById(R.id.progress_text);
            TextView smaxDur = nThreads >1 ? activity.findViewById(R.id.multi_thread_duration) : activity.findViewById(R.id.single_thread_duration);
            ProgressBar progressBar = activity.findViewById(R.id.progressBar);

            // Update the text views.
            sprogressText.setText(values[0] + "%");
            progressBar.setProgress(values[0]);
            if (maxIterationDuration<values[1]){
                maxIterationDuration=values[1];
                smaxDur.setText("Max Duration Per Iteration: "+ maxIterationDuration + "ms");
            }
        }

        @Override
        protected void onPostExecute(Long result) {
            // Display the benchmark result.
            MainActivity activity = activityReference.get();
            if (activity == null || activity.isFinishing()) return;
            TextView resultView = nThreads >1 ?  activity.findViewById(R.id.multi_thread_text) : activity.findViewById(R.id.single_thread_text);
            TextView stateText =  activity.findViewById(R.id.state_text);
            TextView smaxDur = nThreads >1 ? activity.findViewById(R.id.multi_thread_duration) : activity.findViewById(R.id.single_thread_duration);
            Button button1 = activity.findViewById(R.id.button1);
            Button button2 = activity.findViewById(R.id.button2);
            ProgressBar progressBar = activity.findViewById(R.id.progressBar);
            TextView iterCount = nThreads >1 ? activity.findViewById(R.id.multi_thread_iter) : activity.findViewById(R.id.single_thread_iter);


            iterCount.setText("Iterations: "+ totIterations.toString());
            progressBar.setProgress(0);
            smaxDur.setText("Max Duration Per Iteration: "+ maxIterationDuration + "ms");
            resultView.setText("Score: " + result.toString());
            stateText.setText("Not Running Any Test");
            button1.setClickable(true);
            button2.setClickable(true);
        }
    }

    private static void performCpuBenchmark(int nThreads) {
        // Create an executor service to run the CPU benchmark on multiple threads.
        ExecutorService executorService = Executors.newFixedThreadPool(nThreads);

        // Create a list to hold the results of the benchmark.
        List<Future<Float>> results = new ArrayList<>();

        // Submit the benchmark to the executor service for each thread.
        for (int i = 0; i < nThreads; i++) {
            results.add(executorService.submit(new CpuBenchmark()));
        }

        // Wait for the benchmark to complete on each thread.
        for (Future<Float> result : results) {
            try {
                result.get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }
    }

    private static class CpuBenchmark implements Callable<Float> {
        @Override
        public Float call() {
            // Performing a large number of floating-point calculations in a loop.
            // This will stress the CPU and allow us to measure its performance.
            float sum = 0;
            for (int i = 0; i < 100000000; i++) {
                sum += Math.sqrt(i);
            }
            return sum;
        }
    }
}