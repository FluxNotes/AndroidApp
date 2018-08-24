package org.mitre.fluxnotes

import android.content.Context
import android.graphics.Rect
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.widget.CardView
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
        val dataset: JSONObject = JSONObject(intent.getStringExtra("RESULT"))
        viewAdapter = ToxicityAdapter(dataset)

        recyclerView = findViewById(R.id.toxicityList) as RecyclerView
        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager = viewManager
        recyclerView.adapter = viewAdapter
        recyclerView.addItemDecoration(ToxicityDecoration(this))

        val tv = findViewById(R.id.diseaseStatus) as TextView
        if (dataset.getJSONArray("diseaseStatus").length() > 0) {
            tv.text = dataset.getJSONArray("diseaseStatus").getJSONArray(0).getString(0)
        } else {
            tv.text = "Unknown"
        }

    }

    class ToxicityAdapter(private val dataset: JSONObject) :
            RecyclerView.Adapter<ToxicityAdapter.ViewHolder>() {

        class ViewHolder(val cardView: CardView?) : RecyclerView.ViewHolder(cardView)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val cardView = LayoutInflater.from(parent.context)
                    .inflate(R.layout.toxicity_cardview_layout, parent, false) as CardView

            return ViewHolder(cardView)
        }

        override fun onBindViewHolder(holder: ViewHolder?, position: Int) {
            val tv = holder?.cardView?.findViewById(R.id.toxicity) as TextView
            tv.text = dataset.getJSONArray("toxicity").getJSONObject(position).getString("analyzed_text")

            for (i in 0..(dataset.getJSONArray("toxicity").length() - 1)) {
                val toxicity = dataset.getJSONArray("toxicity").getJSONObject(i)

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
