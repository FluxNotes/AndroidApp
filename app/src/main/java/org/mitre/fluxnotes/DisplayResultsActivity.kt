package org.mitre.fluxnotes

import android.content.Context
import android.graphics.Rect
import android.graphics.Typeface
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.constraint.ConstraintLayout
import android.support.constraint.ConstraintSet
import android.support.v7.widget.CardView
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.text.util.Linkify
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
//import kotlinx.android.synthetic.main.item_result.view.*
import org.json.JSONObject

class DisplayResultsActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var viewAdapter: RecyclerView.Adapter<*>
    private lateinit var viewManager: RecyclerView.LayoutManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_display_results)

        viewManager = LinearLayoutManager(this)
        val dataset: JSONObject = JSONObject(intent.getStringExtra("text"))
        viewAdapter = ToxicityAdapter(dataset)

        recyclerView = findViewById(R.id.toxicityList) as RecyclerView
        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager = viewManager
        recyclerView.adapter = viewAdapter
        recyclerView.addItemDecoration(ToxicityDecoration(this))

        val tv = findViewById(R.id.diseaseStatus) as TextView
        if (dataset.getJSONArray("diseaseStatus").length() > 0) {
            tv.text = dataset.getJSONArray("diseaseStatus").getJSONArray(0).getString(0).capitalize()
        } else {
            tv.text = "Unknown"
        }

    }

    inner class ToxicityAdapter(private val dataset: JSONObject) :
            RecyclerView.Adapter<ToxicityAdapter.ViewHolder>() {

        inner class ViewHolder(val cardView: CardView) : RecyclerView.ViewHolder(cardView)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val cardView = LayoutInflater.from(parent.context)
                    .inflate(R.layout.toxicity_cardview_layout, parent, false) as CardView

            return ViewHolder(cardView)
        }

        override fun onBindViewHolder(holder: ViewHolder?, position: Int) {
            val toxicity = dataset.getJSONArray("toxicity").getJSONObject(position)

            // analyzed text
            val tv = holder?.cardView?.findViewById(R.id.toxicity) as TextView
            tv.text = toxicity.getString("analyzed_text")

            val layout = holder?.cardView?.findViewById(R.id.concepts_container) as ConstraintLayout
            val set = ConstraintSet()

            var startId = layout.id
            // concepts
            val context = this@DisplayResultsActivity
            for (i in 0..(toxicity.getJSONArray("concepts").length() - 1)) {
                val concept = toxicity.getJSONArray("concepts").getJSONObject(i)

                val concept_label_tv = TextView(context).apply {
                    id = View.generateViewId()
                    text = "Concept: "
                    typeface = Typeface.DEFAULT_BOLD
                }
                val concept_tv = TextView(context).apply {
                    id = View.generateViewId()
                    text = concept.getString("text")
                }
                val dbpedia_label_tv = TextView(context).apply {
                    id = View.generateViewId()
                    text = "DBpedia: "
                    typeface = Typeface.DEFAULT_BOLD
                }
                val dbpedia_tv = TextView(context).apply {
                    id = View.generateViewId()
                    text = concept.getString("dbpedia_resource")
                }
                val relevance_label_tv = TextView(context).apply {
                    id = View.generateViewId()
                    text = "Relevance: "
                    typeface = Typeface.DEFAULT_BOLD
                }
                val relevance_tv = TextView(context).apply {
                    id = View.generateViewId()
                    text = concept.getString("relevance")
                }

                Linkify.addLinks(dbpedia_tv, Linkify.WEB_URLS)

                layout.addView(concept_label_tv)
                layout.addView(concept_tv)
                layout.addView(dbpedia_label_tv)
                layout.addView(dbpedia_tv)
                layout.addView(relevance_label_tv)
                layout.addView(relevance_tv)

                set.clone(layout)

                set.connect(concept_label_tv.id, ConstraintSet.TOP, startId, if (startId == layout.id) ConstraintSet.TOP else ConstraintSet.BOTTOM, 16)
                set.connect(concept_tv.id, ConstraintSet.START, concept_label_tv.id, ConstraintSet.END, 8)
                set.connect(concept_tv.id, ConstraintSet.TOP, startId, if (startId == layout.id) ConstraintSet.TOP else ConstraintSet.BOTTOM, 16)

                set.connect(dbpedia_label_tv.id, ConstraintSet.TOP, concept_label_tv.id, ConstraintSet.BOTTOM, 8)
                set.connect(dbpedia_tv.id, ConstraintSet.START, dbpedia_label_tv.id, ConstraintSet.END, 8)
                set.connect(dbpedia_tv.id, ConstraintSet.TOP, concept_tv.id, ConstraintSet.BOTTOM, 8)

                set.connect(relevance_label_tv.id, ConstraintSet.TOP, dbpedia_label_tv.id, ConstraintSet.BOTTOM, 8)
                set.connect(relevance_tv.id, ConstraintSet.START, relevance_label_tv.id, ConstraintSet.END, 8)
                set.connect(relevance_tv.id, ConstraintSet.TOP, dbpedia_tv.id, ConstraintSet.BOTTOM, 8)

                set.applyTo(layout)

                startId = relevance_label_tv.id
            }

        }

        override fun getItemCount(): Int {
            return dataset.getJSONArray("toxicity").length()
        }



    }

    class ToxicityDecoration(context: Context) : RecyclerView.ItemDecoration() {
        override fun getItemOffsets(outRect: Rect, view: View?, parent: RecyclerView?, state: RecyclerView.State?) {
            //super.getItemOffsets(outRect, view, parent, state)
            outRect.set(8, 8, 8, 8)
        }
    }


}
