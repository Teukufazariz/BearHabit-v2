package com.santtuhyvarinen.habittracker.views

import android.content.Context
import android.graphics.*
import android.text.TextPaint
import android.util.AttributeSet
import android.view.View
import androidx.core.content.withStyledAttributes
import com.santtuhyvarinen.habittracker.R
import com.santtuhyvarinen.habittracker.models.ChartDataModel

class ChartView(context: Context, attributeSet: AttributeSet) : View(context, attributeSet) {

    companion object {
        const val CHART_TYPE_LINE = 0
        const val CHART_TYPE_COLUMN = 1
    }

    var chartType = CHART_TYPE_LINE

    var chartData : List<ChartDataModel> = ArrayList()

    private val paint = Paint()
    private val textPaint = TextPaint()
    private val textBounds = Rect()

    var columns = 7
    set(value) {
        field = value.coerceAtLeast(0)
    }
    var rows = 5
    set(value) {
        field = value.coerceAtLeast(0)
    }

    var lineColor = Color.BLACK
    var lineStrokeWidth = 10f

    var backgroundLineColor = Color.GRAY
    var backgroundLineStrokeWidth = 2f

    var dotRadius = 15f
    var columnCornerRadius = 10f
    var columnMaxWidth = 0f

    init {
        context.withStyledAttributes(attributeSet, R.styleable.ChartView) {
            lineColor = getColor(R.styleable.ChartView_lineColor, Color.BLACK)
            lineStrokeWidth = getDimension(R.styleable.ChartView_lineStrokeWidth, 10f)
            backgroundLineColor = getColor(R.styleable.ChartView_backgroundLineColor, Color.GRAY)
            backgroundLineStrokeWidth = getDimension(R.styleable.ChartView_backgroundLineStrokeWidth, 2f)
            chartType = getInt(R.styleable.ChartView_chartType, CHART_TYPE_LINE)

            textPaint.textSize = getDimension(R.styleable.ChartView_textSize, 18f)
            textPaint.color = getColor(R.styleable.ChartView_textColor, Color.BLACK)

            columnMaxWidth = getDimension(R.styleable.ChartView_columnMaxWidth, columnMaxWidth)
            columnCornerRadius = getDimension(R.styleable.ChartView_columnCornerRadius, columnCornerRadius)
            dotRadius = getDimension(R.styleable.ChartView_dotRadius, dotRadius)
            columns = getInt(R.styleable.ChartView_columns, columns)
            rows = getInt(R.styleable.ChartView_rows, rows)
        }

        paint.isAntiAlias = true
        paint.style = Paint.Style.FILL
        paint.strokeJoin = Paint.Join.ROUND
        paint.strokeCap = Paint.Cap.ROUND

        textPaint.isAntiAlias = true
        textPaint.textAlign = Paint.Align.CENTER
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val textSize = textPaint.textSize
        val columnWidth = (width - paddingLeft - paddingRight) / columns
        val rowHeight = (height - paddingTop - paddingBottom - textSize*2) / rows

        val bottomHeight = (height - paddingBottom - textSize*2)
        val textY = bottomHeight + textSize

        //Background lines
        paint.color = backgroundLineColor
        paint.strokeWidth = backgroundLineStrokeWidth

        canvas.drawLine(paddingLeft.toFloat(), bottomHeight, width - paddingRight.toFloat(), bottomHeight, paint)

        //Use minimized labels if the column labels won't fit the view
        val useMinimizedLabels = shouldUseMinimizedLabels()

        //Draw columns
        for(column in 0 until columns) {
            //Column lines
            val columnX = paddingLeft + (column * columnWidth) + columnWidth / 2f
            canvas.drawLine(columnX, bottomHeight, columnX, paddingTop.toFloat(), paint)

            if(useMinimizedLabels) {
                if(column > 0 && column < columns-1) continue
            }
            
            //Column labels
            val labelText = if(column < chartData.size) chartData[column].label else continue
            val underLabelText = if(column < chartData.size) chartData[column].underLabel else ""

            //Draw label
            textPaint.typeface = Typeface.DEFAULT_BOLD
            canvas.drawText(labelText, columnX, textY, textPaint)
            textPaint.typeface = Typeface.DEFAULT

            //Draw under label
            canvas.drawText(underLabelText, columnX, textY + textSize, textPaint)
        }

        paint.color = lineColor
        paint.strokeWidth = lineStrokeWidth

        when(chartType) {
            CHART_TYPE_LINE -> {
                //Draw line chart data
                var previousX = 0f
                var previousY = bottomHeight
                for (i in 0 until columns) {
                    val value = if(i < chartData.size) chartData[i].value else 0

                    val x = paddingLeft + (i * columnWidth) + columnWidth / 2f
                    val y = bottomHeight - (rowHeight * value)

                    if(i > 0) canvas.drawLine(previousX, previousY, x, y, paint)

                    canvas.drawCircle(x, y, dotRadius, paint)

                    previousX = x
                    previousY = y
                }
            }

            CHART_TYPE_COLUMN -> {
                //Draw columns chart data
                var columnDataWidth = columnWidth - (columnWidth/8f)
                if(columnMaxWidth > 0f) {
                    columnDataWidth = columnDataWidth.coerceAtMost(columnMaxWidth)
                }

                for (i in 0 until columns) {
                    val value = if(i < chartData.size) chartData[i].value else 0

                    val left = (paddingLeft + (i * columnWidth).toFloat() + (columnWidth/2f)) - (columnDataWidth/2f)
                    val top = bottomHeight - (rowHeight * value)
                    val right = left + columnDataWidth
                    val bottom = bottomHeight

                    canvas.drawRoundRect(left, top, right, bottom, columnCornerRadius, columnCornerRadius, paint)
                }
            }
        }


        //Draw value texts
        for (i in 0 until columns) {
            val value = if (i < chartData.size) chartData[i].value else 0
            val label = value.toString()

            val x = paddingLeft + (i * columnWidth) + columnWidth / 2f
            val y = bottomHeight - (rowHeight * value)

            textPaint.getTextBounds(label, 0, label.length, textBounds)
            canvas.drawText(label, x, y - dotRadius*3 - textBounds.exactCenterY(), textPaint)
        }
    }

    //Checks if the label text will fit the columns
    private fun shouldUseMinimizedLabels() : Boolean {
        if(chartData.isEmpty()) return false

        val underLabelText = chartData[0].underLabel
        textPaint.getTextBounds(underLabelText, 0, underLabelText.length, textBounds)

        val labelsEstimatedWidth = textBounds.width() * chartData.size

        return labelsEstimatedWidth >= width
    }
}