package randomappsinc.com.sqlpractice.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;

import butterknife.BindString;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import randomappsinc.com.sqlpractice.R;
import randomappsinc.com.sqlpractice.activities.AnswerCheckerActivity;
import randomappsinc.com.sqlpractice.adapters.QueryACAdapter;
import randomappsinc.com.sqlpractice.database.AnswerServer;
import randomappsinc.com.sqlpractice.database.DataSource;
import randomappsinc.com.sqlpractice.database.QuestionServer;
import randomappsinc.com.sqlpractice.database.SchemaServer;
import randomappsinc.com.sqlpractice.database.models.ResultSet;
import randomappsinc.com.sqlpractice.utils.Constants;
import randomappsinc.com.sqlpractice.utils.Utils;

public class QuestionFragment extends Fragment {

    public static QuestionFragment create(int position) {
        QuestionFragment questionFragment = new QuestionFragment();
        Bundle bundle = new Bundle();
        bundle.putInt(Constants.QUESTION_NUMBER_KEY, position);
        questionFragment.setArguments(bundle);
        return questionFragment;
    }

    // Question form views
    @BindView(R.id.table_design) TextView tableDesign;
    @BindView(R.id.problem) TextView questionPrompt;
    @BindView(R.id.query_entry) AutoCompleteTextView queryHelper;
    @BindView(R.id.parent) View parent;

    @BindString(R.string.invalid_select) String invalidSelect;

    private SchemaServer schemaServer;
    private QuestionServer questionServer;
    private int currentQuestion;
    private DataSource dataSource;
    private MaterialDialog answerDialog;
    private Unbinder unbinder;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.question_form, container, false);
        unbinder = ButterKnife.bind(this, rootView);

        schemaServer = SchemaServer.getSchemaServer();
        questionServer = QuestionServer.getQuestionServer();

        currentQuestion = getArguments().getInt(Constants.QUESTION_NUMBER_KEY, 0);
        setUpQuestion();

        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        dataSource = new DataSource(getActivity());
        final String answer = AnswerServer.getAnswer(currentQuestion);
        answerDialog = new MaterialDialog.Builder(getActivity())
                .title(R.string.our_answer_query)
                .content(answer)
                .positiveText(android.R.string.yes)
                .neutralText(R.string.copy_answer)
                .onNeutral(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        Utils.copyTextToClipboard(answer, getActivity());
                        Utils.showSnackbar(parent, getString(R.string.copy_confirmation));
                    }
                })
                .build();
    }

    // Sets up a question given the number
    private void setUpQuestion() {
        // Get descriptions of the tables we're supposed to use.
        StringBuilder tableDescriptions = new StringBuilder();
        int[] relevantTables = questionServer.getQuestion(currentQuestion).giveNeededTables();
        for (int i = 0; i < relevantTables.length; i++) {
            if (i != 0) {
                tableDescriptions.append("\n\n");
            }
            tableDescriptions.append(schemaServer.serveTable(relevantTables[i]).getDescription());
        }
        tableDesign.setText(tableDescriptions);

        // Load the problem
        questionPrompt.setText(questionServer.getQuestion(currentQuestion).giveQuestionText());

        // Set up autocomplete
        QueryACAdapter adapter = new QueryACAdapter(getActivity(), android.R.layout.simple_dropdown_item_1line,
                schemaServer.serveSomeTables(relevantTables), queryHelper);
        queryHelper.setAdapter(adapter);
        queryHelper.setText("");
    }

    @OnClick(R.id.submit_query)
    public void checkAnswer() {
        Utils.hideKeyboard(getActivity());
        String userQuery = queryHelper.getText().toString().trim();
        ResultSet results = dataSource.getResultsOfQuery(userQuery);
        if (results.getData() != null) {
            Intent intent = new Intent(getActivity(), AnswerCheckerActivity.class);
            intent.putExtra(Constants.QUESTION_NUMBER_KEY, currentQuestion);
            intent.putExtra(Constants.USER_QUERY_KEY, userQuery);
            getActivity().startActivityForResult(intent, 1);
        } else {
            Utils.showLongSnackbar(parent, String.format(invalidSelect, results.getException()));
        }
    }

    @OnClick(R.id.view_answer)
    public void giveUp() {
        answerDialog.show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }
}
