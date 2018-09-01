package com.wanglu.photoviewerlibrary

import android.annotation.SuppressLint
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.view.ViewPager
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.RecyclerView
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AbsListView
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout


@SuppressLint("StaticFieldLeak")
/**
 * Created by WangLu on 2018/7/15.
 */
object PhotoViewer {
    internal var mInterface: ShowImageViewInterface? = null

    private lateinit var imgData: ArrayList<String> // 图片数据
    private lateinit var container: ViewGroup   // 存放图片的容器， ListView/GridView/RecyclerView
    private var currentPage = 0    // 当前页

    /**
     * 小圆点的drawable
     * 下标0的为没有被选中的
     * 下标1的为已经被选中的
     */
    private val mDot = intArrayOf(R.drawable.no_selected_dot, R.drawable.selected_dot)
    /**
     * 存放小圆点的Group
     */
    private var mDotGroup: LinearLayout? = null
    /**
     * 存放没有被选中的小圆点Group和已经被选中小圆点
     */
    private var mFrameLayout: FrameLayout? = null
    /**
     * 选中的小圆点
     */
    private var mSelectedDot: View? = null



    interface ShowImageViewInterface {
        fun show(iv: ImageView, url: String)
    }

    /**
     * 设置显示ImageView的接口
     */
    fun setShowImageViewInterface(i: ShowImageViewInterface): PhotoViewer {
        mInterface = i
        return this
    }


    /**
     * 设置图片数据
     */
    fun setData(data: ArrayList<String>): PhotoViewer {
        imgData = data
        return this
    }

    fun setImgContainer(container: AbsListView): PhotoViewer {
        this.container = container
        return this
    }

    fun setImgContainer(container: RecyclerView): PhotoViewer {
        this.container = container
        return this
    }

    /**
     * 获取itemView
     */
    private fun getItemView(): View {
        return if (container is AbsListView) {
            val absListView = container as AbsListView
            absListView.getChildAt(currentPage - absListView.firstVisiblePosition)
        } else {
            (container as RecyclerView).layoutManager.findViewByPosition(currentPage)
        }
    }

    /**
     * 获取现在查看到的图片的原始位置 (中间)
     */
    private fun getCurrentViewLocation(): IntArray {
        val result = IntArray(2)
        getItemView().getLocationInWindow(result)
        result[0] += getItemView().measuredWidth / 2
        result[1] += getItemView().measuredHeight / 2
        return result
    }


    /**
     * 设置当前页， 从0开始
     */
    fun setCurrentPage(page: Int): PhotoViewer {
        currentPage = page
        return this
    }

    fun start(fragment: Fragment) {
        val activity = fragment.activity!!
        start(activity as AppCompatActivity)
    }


    fun start(activity: AppCompatActivity) {
        show(activity)
    }

    private fun show(activity: AppCompatActivity) {

        val decorView = activity.window.decorView as ViewGroup
        val frameLayout = FrameLayout(activity)

        val photoViewLayout = LayoutInflater.from(activity).inflate(R.layout.activity_photoviewer, null)
        val viewPager = photoViewLayout.findViewById<ViewPager>(R.id.mLookPicVP)

        val fragments = mutableListOf<PhotoViewerFragment>()
        for (i in 0 until imgData.size) {
            val f = PhotoViewerFragment()
            f.exitListener = object : PhotoViewerFragment.OnExitListener {
                override fun exit() {
                    activity.runOnUiThread {
                        frameLayout.removeAllViews()
                        decorView.removeView(frameLayout)
                    }
                }

            }
            val b = Bundle()
            b.putString("pic_data", imgData[i])
            b.putIntArray("exit_location", getCurrentViewLocation())
            b.putIntArray("img_size", intArrayOf(getItemView().measuredWidth, getItemView().measuredHeight))
            f.arguments = b
            fragments.add(f)
        }

        val adapter = PhotoViewerPagerAdapter(fragments, activity.supportFragmentManager)


        viewPager.offscreenPageLimit = 100
        viewPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrollStateChanged(state: Int) {

            }

            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {

                if (mSelectedDot != null && imgData.size > 1) {
                    val dx = mDotGroup!!.getChildAt(1).x - mDotGroup!!.getChildAt(0).x
                    mSelectedDot!!.translationX = position * dx + positionOffset * dx
                }
            }

            override fun onPageSelected(position: Int) {
                currentPage = position

                val b = Bundle()
                b.putString("pic_data", imgData[currentPage])
                b.putIntArray("img_size", intArrayOf(getItemView().measuredWidth, getItemView().measuredHeight))
                b.putIntArray("exit_location", getCurrentViewLocation())
                fragments[position].arguments = b

            }

        })

        viewPager.adapter = adapter
        viewPager.currentItem = currentPage
        frameLayout.addView(photoViewLayout)


        if (imgData.size > 1)
            frameLayout.post {

                /**
                 * 实例化两个Group
                 */
                mFrameLayout = FrameLayout(activity)
                mDotGroup = LinearLayout(activity)

                if (mDotGroup!!.childCount != 0)
                    mDotGroup!!.removeAllViews()
                val dotParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT)
                /**
                 * 未选中小圆点的间距
                 */
                dotParams.rightMargin = Utils.dp2px(activity, 12)

                /**
                 * 创建未选中的小圆点
                 */
                for (i in 0 until imgData.size) {
                    val iv = ImageView(activity)
                    iv.setImageDrawable(activity.resources.getDrawable(mDot[0]))
                    iv.layoutParams = dotParams
                    mDotGroup!!.addView(iv)
                }

                /**
                 * 设置小圆点Group的方向为水平
                 */
                mDotGroup!!.orientation = LinearLayout.HORIZONTAL
                /**
                 * 设置小圆点在中间
                 */
                mDotGroup!!.gravity = Gravity.CENTER or Gravity.BOTTOM
                /**
                 * 两个Group的大小都为match_parent
                 */
                val params = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT)


                params.bottomMargin = Utils.dp2px(activity, 70)
                /**
                 * 首先添加小圆点的Group
                 */
                frameLayout.addView(mDotGroup, params)

                mDotGroup!!.post {

                    if (mSelectedDot == null) {
                        val iv = ImageView(activity)
                        iv.setImageDrawable(activity.resources.getDrawable(mDot[1]))
                        val params = FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                        /**
                         * 设置选中小圆点的左边距
                         */
                        params.leftMargin = mDotGroup!!.getChildAt(0).x.toInt() + dotParams.rightMargin * currentPage + mDotGroup!!.getChildAt(0).width * currentPage
                        params.gravity = Gravity.BOTTOM
                        mFrameLayout!!.addView(iv, params)
                        mSelectedDot = iv
                    }
                    /**
                     * 然后添加包含未选中圆点和选中圆点的Group
                     */
                    frameLayout.addView(mFrameLayout, params)
                }
            }

        decorView.addView(frameLayout, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
    }


}