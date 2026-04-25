package org.autojs.autojs.ui.timing

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.ConnectivityManager
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.RadioButton
import android.widget.Toast
import androidx.core.net.toUri
import com.github.aakira.expandablelayout.ExpandableRelativeLayout
import com.stardust.autojs.execution.ExecutionConfig
import com.stardust.util.BiMap
import com.stardust.util.BiMaps
import com.stardust.util.MapBuilder
import org.autojs.autojs.R
import org.autojs.autojs.databinding.ActivityTimedTaskSettingBinding
import org.autojs.autojs.external.ScriptIntents
import timber.log.Timber
import org.autojs.autojs.external.receiver.DynamicBroadcastReceivers
import org.autojs.autojs.model.script.ScriptFile
import org.autojs.autojs.timing.IntentTask
import org.autojs.autojs.timing.TaskReceiver
import org.autojs.autojs.timing.TimedTask
import org.autojs.autojs.timing.TimedTaskManager
import org.autojs.autojs.ui.BaseActivity
import org.joda.time.LocalDate
import org.joda.time.LocalDateTime
import org.joda.time.LocalTime
import org.joda.time.format.DateTimeFormat
import org.joda.time.format.DateTimeFormatter

class TimedTaskSettingActivity : BaseActivity() {

    private lateinit var binding: ActivityTimedTaskSettingBinding
    private val dayOfWeekCheckBoxes = mutableListOf<CheckBox>()

    private var scriptFile: ScriptFile? = null
    private var timedTask: TimedTask? = null
    private var intentTask: IntentTask? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTimedTaskSettingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val taskId = intent.getLongExtra(EXTRA_TASK_ID, -1)
        if (taskId != -1L) {
            timedTask = TimedTaskManager.getInstance().getTimedTask(taskId)
            if (timedTask != null) {
                scriptFile = ScriptFile(timedTask!!.scriptPath)
            }
        } else {
            val intentTaskId = intent.getLongExtra(EXTRA_INTENT_TASK_ID, -1)
            if (intentTaskId != -1L) {
                intentTask = TimedTaskManager.getInstance().getIntentTask(intentTaskId)
                if (intentTask != null) {
                    scriptFile = ScriptFile(intentTask!!.scriptPath)
                }
            } else {
                val path = intent.getStringExtra(ScriptIntents.EXTRA_KEY_PATH)
                if (path.isNullOrEmpty()) {
                    finish()
                }
                scriptFile = ScriptFile(path)
            }
        }
        setupViews()
    }

    private fun setupViews() {
        setToolbarAsBack(getString(R.string.text_timed_task))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            binding.toolbar.subtitle = scriptFile?.name
        }
        binding.dailyTaskTimePicker.setIs24HourView(true)
        binding.weeklyTaskTimePicker.setIs24HourView(true)
        findDayOfWeekCheckBoxes(binding.weeklyTaskContainer)
        setupClickListeners()
        setUpTaskSettings()
    }

    private fun setupClickListeners() {
        binding.disposableTaskTimeContainer.setOnClickListener { showDisposableTaskTimePicker() }
        binding.disposableTaskDateContainer.setOnClickListener { showDisposableTaskDatePicker() }

        val checkedChangeListener = CompoundButton.OnCheckedChangeListener { button, isChecked ->
            val relativeLayout = findExpandableLayoutOf(button)
            relativeLayout.post {
                if (isChecked) {
                    relativeLayout.expand()
                } else {
                    relativeLayout.collapse()
                }
            }
        }

        binding.dailyTaskRadio.setOnCheckedChangeListener(checkedChangeListener)
        binding.weeklyTaskRadio.setOnCheckedChangeListener(checkedChangeListener)
        binding.disposableTaskRadio.setOnCheckedChangeListener(checkedChangeListener)
        binding.runOnBroadcast.setOnCheckedChangeListener(checkedChangeListener)
    }

    private fun findDayOfWeekCheckBoxes(parent: ViewGroup) {
        for (i in 0 until parent.childCount) {
            val child = parent.getChildAt(i)
            if (child is CheckBox) {
                dayOfWeekCheckBoxes.add(child)
            } else if (child is ViewGroup) {
                findDayOfWeekCheckBoxes(child)
            }
            if (dayOfWeekCheckBoxes.size >= 7) break
        }
    }

    private fun setUpTaskSettings() {
        binding.disposableTaskDate.text = DATE_FORMATTER.print(LocalDate.now())
        binding.disposableTaskTime.text = TIME_FORMATTER.print(LocalTime.now())
        if (timedTask != null) {
            setupTime()
            return
        }
        if (intentTask != null) {
            setupAction()
            return
        }
        binding.dailyTaskRadio.isChecked = true
    }

    private fun setupAction() {
        binding.runOnBroadcast.isChecked = true
        val buttonId = ACTIONS.getKey(intentTask!!.action)
        if (buttonId == null) {
            binding.runOnOtherBroadcast.isChecked = true
            binding.action.setText(intentTask!!.action)
        } else {
            (findViewById<RadioButton>(buttonId)).isChecked = true
        }
    }

    private fun setupTime() {
        if (timedTask!!.isDisposable) {
            binding.disposableTaskRadio.isChecked = true
            binding.disposableTaskTime.text = TIME_FORMATTER.print(timedTask!!.millis)
            binding.disposableTaskDate.text = DATE_FORMATTER.print(timedTask!!.millis)
            return
        }
        val time = LocalTime.fromMillisOfDay(timedTask!!.millis)
        binding.dailyTaskTimePicker.apply {
            hour = time.hourOfDay
            minute = time.minuteOfHour
        }
        binding.weeklyTaskTimePicker.apply {
            hour = time.hourOfDay
            minute = time.minuteOfHour
        }
        if (timedTask!!.isDaily) {
            binding.dailyTaskRadio.isChecked = true
        } else {
            binding.weeklyTaskRadio.isChecked = true
            for (i in dayOfWeekCheckBoxes.indices) {
                dayOfWeekCheckBoxes[i].isChecked = timedTask!!.hasDayOfWeek(i + 1)
            }
        }
    }

    private fun findExpandableLayoutOf(button: CompoundButton): ExpandableRelativeLayout {
        val parent = button.parent as ViewGroup
        for (i in 0 until parent.childCount) {
            if (parent.getChildAt(i) == button) {
                return parent.getChildAt(i + 1) as ExpandableRelativeLayout
            }
        }
        throw IllegalStateException("findExpandableLayout: button = $button, parent = $parent, childCount = ${parent.childCount}")
    }

    private fun showDisposableTaskTimePicker() {
        val time = TIME_FORMATTER.parseLocalTime(binding.disposableTaskTime.text.toString())
        TimePickerDialog(this, { _, hourOfDay, minute ->
            binding.disposableTaskTime.text = TIME_FORMATTER.print(LocalTime(hourOfDay, minute))
        }, time.hourOfDay, time.minuteOfHour, true).show()
    }

    private fun showDisposableTaskDatePicker() {
        val date = DATE_FORMATTER.parseLocalDate(binding.disposableTaskDate.text.toString())
        DatePickerDialog(this, { _, year, month, dayOfMonth ->
            binding.disposableTaskDate.text = DATE_FORMATTER.print(LocalDate(year, month + 1, dayOfMonth))
        }, date.year, date.monthOfYear - 1, date.dayOfMonth).show()
    }

    private fun createTimedTask(): TimedTask? {
        return when {
            binding.disposableTaskRadio.isChecked -> createDisposableTask()
            binding.dailyTaskRadio.isChecked -> createDailyTask()
            else -> createWeeklyTask()
        }
    }

    private fun createWeeklyTask(): TimedTask? {
        var timeFlag: Long = 0
        for (i in dayOfWeekCheckBoxes.indices) {
            if (dayOfWeekCheckBoxes[i].isChecked) {
                timeFlag = timeFlag or TimedTask.getDayOfWeekTimeFlag(i + 1)
            }
        }
        if (timeFlag == 0L) {
            Toast.makeText(this, R.string.text_weekly_task_should_check_day_of_week, Toast.LENGTH_SHORT).show()
            return null
        }
        val time = LocalTime(binding.weeklyTaskTimePicker.hour, binding.weeklyTaskTimePicker.minute)
        return TimedTask.weeklyTask(time, timeFlag, scriptFile!!.path, ExecutionConfig.default)
    }

    private fun createDailyTask(): TimedTask {
        val time = LocalTime(binding.dailyTaskTimePicker.hour, binding.dailyTaskTimePicker.minute)
        return TimedTask.dailyTask(time, scriptFile!!.path, ExecutionConfig())
    }

    private fun createDisposableTask(): TimedTask? {
        val time = TIME_FORMATTER.parseLocalTime(binding.disposableTaskTime.text.toString())
        val date = DATE_FORMATTER.parseLocalDate(binding.disposableTaskDate.text.toString())
        val dateTime = LocalDateTime(date.year, date.monthOfYear, date.dayOfMonth,
            time.hourOfDay, time.minuteOfHour)
        if (dateTime.isBefore(LocalDateTime.now())) {
            Toast.makeText(this, R.string.text_disposable_task_time_before_now, Toast.LENGTH_SHORT).show()
            return null
        }
        return TimedTask.disposableTask(dateTime, scriptFile!!.path, ExecutionConfig.default)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_timed_task_setting, menu)
        return true
    }

    @SuppressLint("BatteryLife")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_done) {
            if (!(getSystemService(POWER_SERVICE) as PowerManager).isIgnoringBatteryOptimizations(packageName)) {
                try {
                    startActivityForResult(
                        Intent().setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                            .setData("package:$packageName".toUri()),
                        REQUEST_CODE_IGNORE_BATTERY
                    )
                } catch (e: ActivityNotFoundException) {
                    Timber.e(e)
                    createOrUpdateTask()
                }
            } else {
                createOrUpdateTask()
            }
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CODE_IGNORE_BATTERY) {
            Log.d(LOG_TAG, "result code = $requestCode")
            createOrUpdateTask()
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun createOrUpdateTask() {
        if (binding.runOnBroadcast.isChecked) {
            createOrUpdateIntentTask()
            return
        }
        val task = createTimedTask() ?: return
        if (timedTask == null) {
            TimedTaskManager.getInstance().addTask(task)
            if (intentTask != null) {
                TimedTaskManager.getInstance().removeTask(intentTask!!)
            }
            Toast.makeText(this, R.string.text_already_create, Toast.LENGTH_SHORT).show()
        } else {
            task.id = timedTask!!.id
            TimedTaskManager.getInstance().updateTask(task)
        }
        finish()
    }

    private fun createOrUpdateIntentTask() {
        val buttonId = binding.broadcastGroup.checkedRadioButtonId
        if (buttonId == -1) {
            Toast.makeText(this, R.string.error_empty_selection, Toast.LENGTH_SHORT).show()
            return
        }
        val action: String = if (buttonId == R.id.run_on_other_broadcast) {
            val actionText = binding.action.text.toString()
            if (actionText.isEmpty()) {
                binding.action.error = getString(R.string.text_should_not_be_empty)
                return
            }
            actionText
        } else {
            ACTIONS[buttonId]!!
        }
        val task = IntentTask()
        task.action = action
        task.scriptPath = scriptFile!!.path
        task.isLocal = action == DynamicBroadcastReceivers.ACTION_STARTUP
        if (intentTask != null) {
            task.id = intentTask!!.id
            TimedTaskManager.getInstance().updateTask(task)
            Toast.makeText(this, R.string.text_already_create, Toast.LENGTH_SHORT).show()
        } else {
            TimedTaskManager.getInstance().addTask(task)
            if (timedTask != null) {
                TimedTaskManager.getInstance().removeTask(timedTask!!)
            }
        }
        finish()
    }

    companion object {
        const val EXTRA_INTENT_TASK_ID = "intent_task_id"
        const val EXTRA_TASK_ID = TaskReceiver.EXTRA_TASK_ID

        private val TIME_FORMATTER: DateTimeFormatter = DateTimeFormat.forPattern("HH:mm")
        private val DATE_FORMATTER: DateTimeFormatter = DateTimeFormat.forPattern("yy-MM-dd")
        private const val REQUEST_CODE_IGNORE_BATTERY = 27101
        private const val LOG_TAG = "TimedTaskSettings"

        @JvmField
        val ACTION_DESC_MAP: Map<String, Int> = MapBuilder<String, Int>()
            .put(DynamicBroadcastReceivers.ACTION_STARTUP, R.string.text_run_on_startup)
            .put(Intent.ACTION_BOOT_COMPLETED, R.string.text_run_on_boot)
            .put(Intent.ACTION_SCREEN_OFF, R.string.text_run_on_screen_off)
            .put(Intent.ACTION_SCREEN_ON, R.string.text_run_on_screen_on)
            .put(Intent.ACTION_USER_PRESENT, R.string.text_run_on_screen_unlock)
            .put(Intent.ACTION_BATTERY_CHANGED, R.string.text_run_on_battery_change)
            .put(Intent.ACTION_POWER_CONNECTED, R.string.text_run_on_power_connect)
            .put(Intent.ACTION_POWER_DISCONNECTED, R.string.text_run_on_power_disconnect)
            .put("android.net.conn.CONNECTIVITY_CHANGE", R.string.text_run_on_conn_change)
            .put(Intent.ACTION_PACKAGE_ADDED, R.string.text_run_on_package_install)
            .put(Intent.ACTION_PACKAGE_REMOVED, R.string.text_run_on_package_uninstall)
            .put(Intent.ACTION_PACKAGE_REPLACED, R.string.text_run_on_package_update)
            .put(Intent.ACTION_HEADSET_PLUG, R.string.text_run_on_headset_plug)
            .put(Intent.ACTION_CONFIGURATION_CHANGED, R.string.text_run_on_config_change)
            .put(Intent.ACTION_TIME_TICK, R.string.text_run_on_time_tick)
            .build()

        val ACTIONS: BiMap<Int, String> = BiMaps.newBuilder<Int, String>()
            .put(R.id.run_on_startup, DynamicBroadcastReceivers.ACTION_STARTUP)
            .put(R.id.run_on_boot, Intent.ACTION_BOOT_COMPLETED)
            .put(R.id.run_on_screen_off, Intent.ACTION_SCREEN_OFF)
            .put(R.id.run_on_screen_on, Intent.ACTION_SCREEN_ON)
            .put(R.id.run_on_screen_unlock, Intent.ACTION_USER_PRESENT)
            .put(R.id.run_on_battery_change, Intent.ACTION_BATTERY_CHANGED)
            .put(R.id.run_on_power_connect, Intent.ACTION_POWER_CONNECTED)
            .put(R.id.run_on_power_disconnect, Intent.ACTION_POWER_DISCONNECTED)
            .put(R.id.run_on_conn_change, "android.net.conn.CONNECTIVITY_CHANGE")
            .put(R.id.run_on_package_install, Intent.ACTION_PACKAGE_ADDED)
            .put(R.id.run_on_package_uninstall, Intent.ACTION_PACKAGE_REMOVED)
            .put(R.id.run_on_package_update, Intent.ACTION_PACKAGE_REPLACED)
            .put(R.id.run_on_headset_plug, Intent.ACTION_HEADSET_PLUG)
            .put(R.id.run_on_config_change, Intent.ACTION_CONFIGURATION_CHANGED)
            .put(R.id.run_on_time_tick, Intent.ACTION_TIME_TICK)
            .build()
    }
}
