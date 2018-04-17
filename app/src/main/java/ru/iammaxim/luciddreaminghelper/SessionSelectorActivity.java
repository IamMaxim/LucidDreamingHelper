package ru.iammaxim.luciddreaminghelper;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;

public class SessionSelectorActivity extends AppCompatActivity {
    private Adapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_session_selector);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(view -> {
            startActivity(new Intent(this, RecorderActivity.class));
            finish();
        });

        RecyclerView rv = findViewById(R.id.rv);
        adapter = new Adapter();
        rv.setAdapter(adapter);
        rv.setLayoutManager(new LinearLayoutManager(this));

        File dir = new File(mSensorManager.output_dir);
        if (!dir.exists() || dir.listFiles() == null)
            return;
        for (File f : dir.listFiles()) {
            adapter.elements.add(new Element(f.getName(), f.getPath()));
        }
        adapter.notifyDataSetChanged();
    }

    public class Adapter extends RecyclerView.Adapter<Adapter.ViewHolder> implements View.OnClickListener {
        public ArrayList<Element> elements = new ArrayList<>();

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new ViewHolder(View.inflate(getApplicationContext(), R.layout.element_sessionselector_file, null));
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            holder.root.setId(position);
            holder.name.setText(elements.get(position).name);
        }

        @Override
        public int getItemCount() {
            return elements.size();
        }

        @Override
        public void onClick(View v) {
            Element e = elements.get(v.getId());
            Bundle bundle = new Bundle();
            bundle.putString("file", e.path);
            startActivity(new Intent(getApplicationContext(), SessionViewerActivity.class).putExtras(bundle));
        }

        public class ViewHolder extends RecyclerView.ViewHolder {

            public TextView name;
            public View root;

            public ViewHolder(View itemView) {
                super(itemView);
                name = itemView.findViewById(R.id.name);
                root = itemView;
                root.setOnClickListener(Adapter.this);
            }

        }
    }

    public class Element {
        String name, path;

        public Element(String name, String path) {
            this.name = name;
            this.path = path;
        }
    }
}
