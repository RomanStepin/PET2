package com.example.pet2

import android.content.Context
import android.content.SharedPreferences
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Toast
import androidx.annotation.NonNull
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import fito.Fito
import fito.FitoChanels
import fito.FitoCmd
import fito.FitoParam
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_blank.*
import kotlinx.android.synthetic.main.fragment_blank2.*
import kotlinx.android.synthetic.main.fragment_blank3.*
import kotlinx.android.synthetic.main.fragment_blank5.*
import kotlinx.android.synthetic.main.fragment_lan_setting.*
import kotlinx.android.synthetic.main.fragment_mqtt_auth.*
import kotlinx.android.synthetic.main.fragment_power.*
import kotlinx.android.synthetic.main.fragment_restore.*
import kotlinx.android.synthetic.main.fragment_sys_i_d.*
import kotlinx.android.synthetic.main.fragment_time.*
import kotlinx.android.synthetic.main.fragment_wifi_auth.*
import java.io.IOException
import java.math.BigInteger
import java.net.*
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList


class MainActivity : AppCompatActivity(), View.OnClickListener, CHECK_TOAST {

    lateinit var toast: Toast
    var port: Int = 4096
    lateinit var receiverUdp: ReceiverUDP

    var broadcastAddress: InetAddress
        get() {
            val wifi = getSystemService(Context.WIFI_SERVICE) as WifiManager
            val dhcp = wifi.dhcpInfo?: return InetAddress.getByName("255.255.255.255")
            val broadcast = dhcp.ipAddress and dhcp.netmask or dhcp.netmask.inv()
            val quads = ByteArray(4)
            for (k in 0..3) quads[k] = (broadcast shr k * 8 and 0xFF).toByte()
            val s1: InetAddress = InetAddress.getByAddress(quads)
            return s1
        }
        set(value) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    override fun onStart() {
        super.onStart()

        viewpager_main.adapter = MainViewPagerAdapter(this.supportFragmentManager)
        tabLayout_main.setupWithViewPager(viewpager_main)

        button_broadcast.setOnClickListener(this)

        val sh: SharedPreferences = getSharedPreferences("ips", MODE_PRIVATE)
        val ip = sh.getString("ip", "")
        val ipH = sh.getString("ipH", "")
        val tid = sh.getString("tid", "42")
        val sid = sh.getString("sid", "0")

        ignored_ip_text.setText(ip)
        ignored_ip_heartbeat_text.setText(ipH)
        targetId_text.setText(tid)
        sysId_text.setText(sid)

        receiverUdp = ReceiverUDP(this, port, this)
        receiverUdp.start()
    }

    fun send()
    {
        object: Thread(){
            override fun run() {
                val fito: Fito.MessageUnion? = Fito.MessageUnion.newBuilder().apply {
                    sysId = 0
                    targetId = 5
                    param = FitoParam.Param.newBuilder().apply {
                        action = FitoParam.Param.Action.SET
                        ack = FitoParam.Param.Ack.ACK_ACCEPTED
                        wifiAuth = FitoParam.WiFiAuth.newBuilder().apply {
                            ssid = "Kakadu-2G"
                            password = "mCQ5p8hz"
                        }.build()
                    }.build()
                }.build()

                val clientSocket = DatagramSocket()
                clientSocket.broadcast = true
                val sendDataU1 = fito?.toByteArray()
                val sendPacketU1 = sendDataU1?.size?.let { DatagramPacket(
                    sendDataU1,
                    it,
                    broadcastAddress,
                    4096
                ) }
                clientSocket.send(sendPacketU1)
            }
        }.start()
    }

    fun send1()
    {
        var sysID = Integer.parseInt(sysId_text.text.toString())
        var targetID = Integer.parseInt(targetId_text.text.toString())
        val dateFormat: DateFormat = SimpleDateFormat("dd:MM:yyyy:HH:mm:ss", Locale("en"))
        var fito: Fito.MessageUnion



        when (viewpager_main.currentItem) {
            0 -> {
                val fwVersion = Integer.parseInt(fwVersion_text.text.toString())
                val t = (time_text.text.toString())

                val d = dateFormat.parse(t)

                var time = dateFormat.parse(t).time
                var time1 = time / 1000

                val currentTime = Calendar.getInstance().time
                val itime = time1.toInt()


                val power = if (power_switch.isChecked)
                    FitoCmd.Power.newBuilder().apply { state = FitoCmd.Power.State.ON }.build()
                else
                    FitoCmd.Power.newBuilder().apply { state = FitoCmd.Power.State.OFF }.build()

                fito = Fito.MessageUnion.newBuilder().apply {
                    sysId = sysID
                    targetId = targetID;
                    heartbeat = Fito.Heartbeat.newBuilder().apply {
                        this.fwVersion = fwVersion
                        this.power = power
                        this.time = itime
                        this.program = 1
                    }.build()
                }.build()

                val sender = Sender(fito, broadcastAddress)
                sender.start()
            }

            1 -> {
                val action = action_text_cmd.text.toString()
                val ack = ack_text_cmd.text.toString()
                var oneof = 0;
                oneof = viewpager_cmd.currentItem

                fito = Fito.MessageUnion.newBuilder().apply {
                    sysId = sysID
                    targetId = targetID;
                    cmd = FitoCmd.Cmd.newBuilder().apply {
                        this.action = when (action) {
                            "ACK" -> FitoCmd.Cmd.Action.ACK
                            else -> FitoCmd.Cmd.Action.CMD
                        }
                        this.ack = when (ack) {
                            "ACK_ACCEPTED" -> FitoCmd.Cmd.Ack.ACK_ACCEPTED
                            "ACK_VALUE_UNSUPPORTED" -> FitoCmd.Cmd.Ack.ACK_VALUE_UNSUPPORTED
                            "ACK_FAILED" -> FitoCmd.Cmd.Ack.ACK_FAILED
                            else -> FitoCmd.Cmd.Ack.ACK_IN_PROGRESS
                        }
                        when (oneof) {
                            0 -> this.power = FitoCmd.Power.newBuilder().apply {
                                state =
                                    if (power_cmd.isChecked) FitoCmd.Power.State.ON else FitoCmd.Power.State.OFF
                            }.build()
                            1 -> this.reboot = FitoCmd.Reboot.getDefaultInstance()
                            2 -> this.restore = when (restore_text.text.toString()) {
                                "ALL" -> FitoCmd.Restore.newBuilder()
                                    .apply { type = FitoCmd.Restore.Type.ALL }.build()
                                "WIFI" -> FitoCmd.Restore.newBuilder()
                                    .apply { type = FitoCmd.Restore.Type.WiFi }.build()
                                else -> FitoCmd.Restore.newBuilder()
                                    .apply { type = FitoCmd.Restore.Type.LAN }.build()
                            }
                        }
                    }.build()
                }.build()

                val sender = Sender(fito, broadcastAddress)
                sender.start()
            }

            2 -> {
                val action = action_text_param.text.toString()
                val ack = ack_text_param.text.toString()
                var oneof = 0;
                oneof = viewpager_param.currentItem

                fito = Fito.MessageUnion.newBuilder().apply {
                    sysId = sysID
                    targetId = targetID;
                    param = FitoParam.Param.newBuilder().apply {
                        this.action = when (action) {
                            "ACK" -> FitoParam.Param.Action.ACK
                            "GET" -> FitoParam.Param.Action.GET
                            "SET" -> FitoParam.Param.Action.SET
                            else -> FitoParam.Param.Action.VALUE
                        }
                        this.ack = when (ack) {
                            "ACK_ACCEPTED" -> FitoParam.Param.Ack.ACK_ACCEPTED
                            "ACK_VALUE_UNSUPPORTED" -> FitoParam.Param.Ack.ACK_VALUE_UNSUPPORTED
                            "ACK_FAILED" -> FitoParam.Param.Ack.ACK_FAILED
                            else -> FitoParam.Param.Ack.ACK_IN_PROGRESS
                        }
                        when (oneof) {
                            0 -> this.sysID = FitoParam.SysID.newBuilder().apply {
                                sysID = Integer.parseInt(sysId_param_text.text.toString())
                            }.build()
                            1 -> {
                                val dateFormat: DateFormat = SimpleDateFormat(
                                    "dd:MM:yyyy:HH:mm:ss", Locale(
                                        "en"
                                    )
                                )
                                val t = (time_param_text.text.toString())
                                val time = dateFormat.parse(t).time
                               // val currentTime = Calendar.getInstance().time
                              //  val ctime = currentTime.time
                                val ltime: Long = time/1000
                                val itime = ltime.toInt()
                                this.time = FitoParam.Time.newBuilder().apply {
                                    this.time = itime
                                }.build()
                            }
                            2 -> {
                                this.wifiAuth = FitoParam.WiFiAuth.newBuilder().apply {
                                    this.ssid = ssid_text.text.toString()
                                    this.password = password_text.text.toString()
                                }.build()
                            }
                            3 -> {
                                this.lanSetting = FitoParam.LanSetting.newBuilder().apply {
                                    this.useDHCP = DHCP_setting.isChecked
                                    this.gateware = gateware_text.text.toString()
                                    this.mask = mask_text.text.toString()
                                    this.ip = ip_text.text.toString()
                                }.build()
                            }
                            4 -> {
                                this.mqttAuth = FitoParam.MQTTAuth.newBuilder().apply {
                                    this.login = mqtt_login_text.text.toString()
                                    this.password = mqtt_password_text.text.toString()
                                }.build()
                            }
                        }
                    }.build()
                }.build()

                val sender = Sender(fito, broadcastAddress)
                sender.start()
            }
            4 -> {
                val action = action_text_chanels.text.toString()
                val ack = ack_text_chanels.text.toString()
                var oneof = 0;

                fito = Fito.MessageUnion.newBuilder().apply {
                    sysId = sysID
                    targetId = targetID;
                    chanels = FitoChanels.Chanels.newBuilder().apply {
                        this.addAllChanel(
                            mutableListOf(
                                FitoChanels.Chanel.newBuilder().apply {
                                    this.number = 1; this.value = Integer.parseInt(
                                    ack_text_chanel_1.text.toString()
                                )
                                }.build(),
                                FitoChanels.Chanel.newBuilder().apply {
                                    this.number = 2; this.value = Integer.parseInt(
                                    ack_text_chanel_2.text.toString()
                                )
                                }.build(),
                                FitoChanels.Chanel.newBuilder().apply {
                                    this.number = 3; this.value = Integer.parseInt(
                                    ack_text_chanel_3.text.toString()
                                )
                                }.build(),
                                FitoChanels.Chanel.newBuilder().apply {
                                    this.number = 4; this.value = Integer.parseInt(
                                    ack_text_chanel_4.text.toString()
                                )
                                }.build(),
                                FitoChanels.Chanel.newBuilder().apply {
                                    this.number = 5; this.value = Integer.parseInt(
                                    ack_text_chanel_5.text.toString()
                                )
                                }.build(),
                                FitoChanels.Chanel.newBuilder().apply {
                                    this.number = 6; this.value = Integer.parseInt(
                                    ack_text_chanel_6.text.toString()
                                )
                                }.build()
                            )
                        )
                        this.action = when (action) {
                            "ACK" -> FitoChanels.Chanels.Action.ACK
                            "GET" -> FitoChanels.Chanels.Action.GET
                            "SET" -> FitoChanels.Chanels.Action.SET
                            else -> FitoChanels.Chanels.Action.VALUE
                        }
                        this.ack = when (ack) {
                            "ACK_ACCEPTED" -> FitoChanels.Chanels.Ack.ACK_ACCEPTED
                            "ACK_VALUE_UNSUPPORTED" -> FitoChanels.Chanels.Ack.ACK_VALUE_UNSUPPORTED
                            "ACK_FAILED" -> FitoChanels.Chanels.Ack.ACK_FAILED
                            else -> FitoChanels.Chanels.Ack.ACK_IN_PROGRESS
                        }
                    }.build()
                }.build()

                val sender = Sender(fito, broadcastAddress)
                sender.start()
            }
        }



    }

    class Sender(var fitoo: Fito.MessageUnion, var broadcastAddress: InetAddress) : Thread() {
        override fun run() {
            val clientSocket = DatagramSocket()
            clientSocket.broadcast = true
            val sendDataU1 = fitoo.toByteArray()
            val sendPacketU1 = sendDataU1?.size?.let {
                DatagramPacket(
                    sendDataU1,
                    it,
                    broadcastAddress,
                    4096
                )
            }
            clientSocket.send(sendPacketU1)
        }
    }

    override fun onPause() {
        super.onPause()
        receiverUdp.close()
    }




    override fun onClick(v: View?) {
        val sh: SharedPreferences = getSharedPreferences("ips", MODE_PRIVATE)
        val ip = ignored_ip_text.text.toString()
        val ipH = ignored_ip_heartbeat_text.text.toString()
        val tid = targetId_text.text.toString()
        val sid = sysId_text.text.toString()
        sh.edit().apply{ putString("ip", ip); putString("ipH", ipH); putString("tid", tid); putString("sid", sid); apply() }

        if (v != null) {
            when(v.id) {
                R.id.button_broadcast -> {
                    send1()
                }
            }
        }
    }

    override fun getIp(): String {
        return ignored_ip_text.text.toString()
    }

    override fun getHIp(): String {
        return ignored_ip_heartbeat_text.text.toString()
    }

    override fun getMyIp(): Boolean {
        return switch1.isChecked
    }


}

class ReceiverUDP(var context: Context, var port: Int, var check_toast: CHECK_TOAST): Thread()
{
    val socket: DatagramSocket = DatagramSocket(port)
    var wm = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
    var myAddress: String = ""
    var isToast: Boolean = true

    fun close()
    {
        socket.close()
    }

    var toast: Toast = Toast(context)
    override fun run() {
        try {
            socket.broadcast = true
        } catch (e: SocketException) {
            e.printStackTrace()
        }
        while (!socket.isClosed) {
            try {
                val buf = ByteArray(4096)
                val packet = DatagramPacket(buf, 4096)

                socket.receive(packet)

                val fito: Fito.MessageUnion = Fito.MessageUnion.parseFrom(packet.data.copyOf(packet.length))

                val wi: WifiInfo = wm.connectionInfo

                val ipAddress: ByteArray = BigInteger.valueOf(wi.getIpAddress().toLong()).toByteArray()
                ipAddress.reverse()
                try {
                    val myAddr = InetAddress.getByAddress(ipAddress)
                    myAddress = myAddr.hostAddress
                } catch (e: UnknownHostException) {
                    e.printStackTrace()
                }

                val mainHandler = Handler(Looper.getMainLooper())
                val myRunnable = Runnable {
                    toast.cancel()
                    toast = Toast.makeText(
                        context,
                        "  получили сообщение:  sysId = " + fito.toString() + "  с ip адреса " + packet.address.hostAddress,
                        Toast.LENGTH_LONG
                    )

                    isToast = true;
                    if (check_toast.getIp().equals(packet.address.hostAddress)) isToast = false
                    if (check_toast.getHIp().equals(packet.address.hostAddress) && fito.heartbeat.isInitialized) isToast = false
                    if (!check_toast.getMyIp() && myAddress.equals(packet.address.hostAddress)) isToast = false

                    if (isToast )toast.show()
                }


                mainHandler.post(myRunnable);

            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        socket.close()
    }

}

interface CHECK_TOAST
{
    fun getIp(): String
    fun getHIp(): String
    fun getMyIp(): Boolean
}

class MainViewPagerAdapter internal constructor(@NonNull fm: FragmentManager) : FragmentStatePagerAdapter(
    fm,
    BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT
) {

    override fun getCount(): Int {
        return 5
    }

    @NonNull
    override fun getItem(position: Int): Fragment {
        return when (position) {
            0 -> BlankFragment()
            1 -> BlankFragment2()
            2 -> BlankFragment3()
            3 -> BlankFragment4()
            4 -> BlankFragment5()
            else -> BlankFragment()
        }
    }

    override fun getPageTitle(position: Int): CharSequence? {
        return when (position) {
            0 -> "Heartbeat"
            1 -> "Cmd"
            2 -> "Param"
            3 -> "Error"
            4 -> "Chanels"
            else -> "хуй"
        }
    }
}










