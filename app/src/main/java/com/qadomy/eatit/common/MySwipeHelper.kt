package com.qadomy.eatit.common

import android.content.Context
import android.content.res.Resources
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.qadomy.eatit.callback.IMyButtonCallback
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap


/**
 *
 * This class responsible to create swipe for recycler view items to show button
 */

abstract class MySwipeHelper(
    context: Context,
    private val recyclerView: RecyclerView,
    internal var buttonWidth: Int
) : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {

    abstract fun instantisteMyButton(
        viewHolder: RecyclerView.ViewHolder,
        buffer: MutableList<MyButton>
    )


    private var buttonList: MutableList<MyButton>? = null
    private lateinit var gestureDetector: GestureDetector
    private var swipePosition = -1
    private var swipeThreshold = 0.5f
    private val buttonBuffer: MutableMap<Int, MutableList<MyButton>>
    private lateinit var removerQueue: Queue<Int>

    private val gestureListener =
        object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapUp(e: MotionEvent?): Boolean {
                for (button in buttonList!!)
                    if (button.getClick(e!!.x, e!!.y))
                        break

                return true
            }
        }


    // OnTouchListener use when user release touch from screen
    private val onTouchListener = View.OnTouchListener { _, motionEvent ->
        if (swipePosition < 0) return@OnTouchListener false
        val point = Point(motionEvent.rawX.toInt(), motionEvent.rawY.toInt())

        val swipeViewHolder = recyclerView.findViewHolderForAdapterPosition(swipePosition)
        val swipedItem = swipeViewHolder!!.itemView
        val rect = Rect()
        swipedItem.getGlobalVisibleRect(rect)

        if (motionEvent.action == MotionEvent.ACTION_DOWN ||
            motionEvent.action == MotionEvent.ACTION_UP ||
            motionEvent.action == MotionEvent.ACTION_MOVE
        ) {
            if (rect.top < point.y && rect.bottom > point.y)
                gestureDetector.onTouchEvent(motionEvent)
            else {
                removerQueue.add(swipePosition)
                swipePosition = -1
                recoverSwipedItem()
            }
        }
        false

    }

    init {
        this.buttonList = ArrayList()
        this.gestureDetector = GestureDetector(context, gestureListener)
        this.recyclerView.setOnTouchListener(onTouchListener)
        this.buttonBuffer = HashMap()


        removerQueue = object : LinkedList<Int>() {
            override fun add(element: Int): Boolean {
                return if (contains(element))
                    false
                else
                    super.add(element)
            }
        }

        attachSwipe()
    }


    @Synchronized
    private fun recoverSwipedItem() {
        while (!removerQueue.isEmpty()) {
            val pos = removerQueue.poll()!!
            if (pos > -1)
                recyclerView.adapter!!.notifyItemChanged(pos)
        }
    }


    private fun attachSwipe() {
        val itemTouchHelper = ItemTouchHelper(this)
        itemTouchHelper.attachToRecyclerView(recyclerView)
    }


    inner class MyButton(
        private val context: Context,
        private val text: String,
        private val textSize: Int,
        private val imageResId: Int,
        private val color: Int,
        private val listener: IMyButtonCallback
    ) {

        private var pos: Int = 0
        private var clickRegion: RectF? = null
        private val resources: Resources

        init {
            resources = context.resources
        }

        fun getClick(x: Float, y: Float): Boolean {
            if (clickRegion != null && clickRegion!!.contains(x, y)) {
                listener.onClick(pos)
                return true
            }
            return false
        }

        fun onDraw(c: Canvas, rectF: RectF, pos: Int) {
            val p = Paint()
            p.color = color
            c.drawRect(rectF, p)

            p.color = Color.WHITE
            p.textSize = textSize.toFloat()

            val r = Rect()
            val cHeight = rectF.height()
            val cWidth = rectF.width()

            p.textAlign = Paint.Align.LEFT
            p.getTextBounds(text, 0, text.length, r)

            var x = 0f
            var y = 0f

            if (imageResId == 0) {
                // Just Text
                x = cWidth / 2f - r.width() / 2f - r.left.toFloat()
                y = cHeight / 2f + r.height() / 2f - r.bottom
                c.drawText(text, rectF.left + x, rectF.top + y, p)

            } else {
                // if drawable
                val d = ContextCompat.getDrawable(context, imageResId)
                val bitmap = drawableToBitmap(d)
                c.drawBitmap(
                    bitmap,
                    (rectF.left + rectF.right) / 2,
                    (rectF.top + rectF.bottom) / 2,
                    p
                )
            }

            clickRegion = rectF
            this.pos = pos
        }
    }

    // create a bitmap
    private fun drawableToBitmap(d: Drawable?): Bitmap {
        if (d!! is BitmapDrawable) {
            return d.toBitmap()
        }

        val bitmap =
            Bitmap.createBitmap(d!!.intrinsicWidth, d.intrinsicHeight, Bitmap.Config.ARGB_8888)

        val canavas = Canvas(bitmap)

        d.setBounds(0, 0, canavas.width, canavas.height)
        d.draw(canavas)

        return bitmap
    }


    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        return false
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        val pos = viewHolder.adapterPosition
        if (swipePosition != pos)
            removerQueue.add(swipePosition)

        swipePosition = pos

        if (buttonBuffer.containsKey(swipePosition))
            buttonList = buttonBuffer[swipePosition]
        else
            buttonList!!.clear()

        buttonBuffer.clear()
        swipeThreshold = 0.5f * buttonList!!.size.toFloat() * buttonWidth.toFloat()
        recoverSwipedItem()
    }

    override fun getSwipeThreshold(viewHolder: RecyclerView.ViewHolder): Float {
        return swipeThreshold
    }

    override fun getSwipeEscapeVelocity(defaultValue: Float): Float {
        return 0.1f * defaultValue
    }

    override fun getSwipeVelocityThreshold(defaultValue: Float): Float {
        return 5.0f * defaultValue
    }

    override fun onChildDraw(
        c: Canvas,
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        dX: Float,
        dY: Float,
        actionState: Int,
        isCurrentlyActive: Boolean
    ) {
        val pos = viewHolder.adapterPosition
        var translationX = dX
        var itemView = viewHolder.itemView

        if (pos < 0) {
            swipePosition = pos
            return
        }

        if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
            if (dX < 0) {
                var buffer: MutableList<MyButton> = ArrayList()

                if (!buttonBuffer.containsKey(pos)) {
                    instantisteMyButton(viewHolder, buffer)
                    buttonBuffer[pos] = buffer
                } else {
                    buffer = buttonBuffer[pos]!!
                    translationX =
                        dX * buffer.size.toFloat() * buttonWidth.toFloat() / itemView.width
                    drawButton(c, itemView, buffer, pos, translationX)
                }
            }
        }

        super.onChildDraw(c, recyclerView, viewHolder, translationX, dY, actionState, isCurrentlyActive)
    }


    private fun drawButton(
        c: Canvas,
        itemView: View,
        buffer: MutableList<MySwipeHelper.MyButton>,
        pos: Int,
        translationX: Float
    ) {

        var right = itemView.right.toFloat()
        val dButtonWidth = -1 * translationX / buffer.size

        for (button in buffer) {
            val left = right - dButtonWidth
            button.onDraw(
                c,
                RectF(left, itemView.top.toFloat(), right, itemView.bottom.toFloat()),
                pos
            )

            right = left
        }
    }

}