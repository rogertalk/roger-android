package com.rogertalk.roger.realm.explorer


import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.ViewGroup.LayoutParams
import android.widget.*
import com.rogertalk.roger.R
import com.rogertalk.roger.models.realm.CachedAudio
import com.rogertalk.roger.utils.extensions.appHelper
import java.lang.reflect.*
import java.util.*

class DisplayModelActivity : Activity() {
    private var textSize: Int = 0
    private var padding: Int = 0
    private var methods: Array<Method>? = null
    private var model: Class<CachedAudio>? = null
    private var context: Context? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        context = this
        textSize = resources.getDimension(R.dimen.cell_text_size).toInt()
        padding = resources.getDimension(R.dimen.cell_padding).toInt()

        val modelKey = intent.getStringExtra(EXTRA_MODEL_KEY)
        model = CachedAudio::class.java
        methods = model!!.declaredMethods
        Arrays.sort(methods, MemberComparator<Method>())

        val table = TableLayout(this)
        addModelFieldHeaders(table)
        addModelDataRows(table)

        val scrollView = ScrollView(this)
        scrollView.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        scrollView.isHorizontalScrollBarEnabled = true
        scrollView.isVerticalScrollBarEnabled = true
        scrollView.isFillViewport = true

        val horizontalScrollView = HorizontalScrollView(this)
        horizontalScrollView.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        horizontalScrollView.isHorizontalScrollBarEnabled = true
        horizontalScrollView.isVerticalScrollBarEnabled = true
        horizontalScrollView.isFillViewport = true

        scrollView.addView(horizontalScrollView)
        horizontalScrollView.addView(table)

        setContentView(scrollView)
    }

    private fun addModelDataRows(table: TableLayout) {
        val realm = appHelper().getRealm()
        val data = realm.where(model).findAll()
        for (target in data) {
            val row = ColoredTableRow(this, RowColors.getColor(table.childCount))
            val highlight = Color.parseColor("#336699")
            for (method in methods!!) {
                var value = ""
                val cell: TextView
                val returnValue: Any?
                try {
                    if (isAccessor(method.name) && !Modifier.isStatic(method.modifiers)) {
                        returnValue = method.invoke(target)
                        value = if (returnValue == null) "null" else returnValue.toString()
                        cell = CellTextView(this, value, padding, textSize)
                        // TODO : link to other models
                        /*if (Nosey.getInstance(this).isRegistered(method.returnType.simpleName) && returnValue != null) {
                            // Highlight Other Realm Models & Link to them
                            cell.setTextColor(highlight)
                            cell.setOnClickListener { DisplayModelActivity.startActivity(this, method.returnType.simpleName) }
                        }*/
                        row.addView(cell)
                    }
                } catch (e: IllegalAccessException) {
                    e.printStackTrace()
                } catch (e: InvocationTargetException) {
                    e.printStackTrace()
                }

            }
            table.addView(row)
        }
    }

    fun isAccessor(name: String): Boolean {
        return name.startsWith("get") || name.startsWith("is")
    }

    fun addModelFieldHeaders(table: TableLayout) {
        // Get Fields for a given model
        model!!.declaredFields
        val allFields = model!!.declaredFields
        Arrays.sort(allFields, MemberComparator<Field>())

        // Add the field headers to the table row
        val headers = ColoredTableRow(this, RowColors.getColor(table.childCount))
        for (field in allFields) {
            if (!Modifier.isStatic(field.modifiers)) {
                headers.addView(CellTextView(this, field.name, padding, textSize))
            }
        }
        table.addView(headers)
    }

    inner class CellTextView(context: Context, value: String, padding: Int, textSize: Int) : TextView(context) {
        init {
            setPadding(padding, padding, padding, padding)
            setTextSize(textSize.toFloat())
            text = value
        }
    }

    inner class ColoredTableRow(context: Context, color: Int) : TableRow(context) {
        init {
            setBackgroundColor(color)
        }
    }

    inner class MemberComparator<T : Member> : Comparator<T> {
        override fun compare(lhs: T, rhs: T): Int {
            return lhs.name.compareTo(rhs.name)
        }
    }

    object RowColors {
        var OddRowColor = Color.parseColor("#CCCCCC")
        var EvenRowColor = Color.parseColor("#ffffff")

        fun getColor(index: Int): Int {
            if (index % 2 == 0)
                return EvenRowColor
            return OddRowColor
        }
    }

    companion object {

        private val EXTRA_MODEL_KEY = "extra_model_key"

        fun startActivity(context: Context, modelName: String) {
            val intent = Intent(context, DisplayModelActivity::class.java)
            intent.putExtra(EXTRA_MODEL_KEY, modelName)
            context.startActivity(intent)
        }
    }
}
