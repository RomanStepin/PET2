package com.example.pet2

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.annotation.NonNull
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import kotlinx.android.synthetic.main.fragment_blank2.*
import kotlinx.android.synthetic.main.fragment_blank3.*

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [BlankFragment3.newInstance] factory method to
 * create an instance of this fragment.
 */
class BlankFragment3 : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_blank3, container, false)
    }

    override fun onStart() {
        super.onStart()

        val items_action = listOf("ACK", "GET", "SET", "VALUE")
        val adapter_action = ArrayAdapter(requireContext(), android.R.layout.activity_list_item, android.R.id.text1, items_action)
        action_text_param.setAdapter(adapter_action)

        val items_ack = listOf("ACK_ACCEPTED", "ACK_VALUE_UNSUPPORTED", "ACK_FAILED", "ACK_IN_PROGRESS")
        val adapter_ack = ArrayAdapter(requireContext(), android.R.layout.activity_list_item, android.R.id.text1, items_ack)
        ack_text_param.setAdapter(adapter_ack)

        viewpager_param.adapter = ParamViewPagerAdapter(activity!!.supportFragmentManager)
        viewpager_param.currentItem = 1
        tabLayout_param.setupWithViewPager(viewpager_param)

    }

    override fun onPause() {
        super.onPause()
        Log.d("loggg", "Fragment3     onPause")
        viewpager_param.removeAllViews()
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment BlankFragment3.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            BlankFragment3().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}

class ParamViewPagerAdapter internal constructor(@NonNull fm: FragmentManager) : FragmentStatePagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {


    override fun getCount(): Int {
        return 5
    }

    @NonNull
    override fun getItem(position: Int): Fragment {
        return when (position) {
            0 -> fragment_sysID()
            1 -> fragment_time()
            2 -> fragment_wifiAuth()
            3 -> fragment_lanSetting()
            4 -> fragment_mqttAuth()
            else -> fragment_power()
        }
    }

    override fun getPageTitle(position: Int): CharSequence? {
        return when (position) {
            0 -> "sysID"
            1 -> "time"
            2 -> "wifiAuth"
            3 -> "lanSetting"
            4 -> "preset"
            else -> "хуй"
        }
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        super.destroyItem(container, position, `object`)
    }
}