package com.santtuhyvarinen.habittracker.managers

import androidx.lifecycle.MutableLiveData
import com.santtuhyvarinen.habittracker.models.HabitWithTaskLogs
import com.santtuhyvarinen.habittracker.models.TaskLog
import com.santtuhyvarinen.habittracker.models.TaskModel
import com.santtuhyvarinen.habittracker.utils.CalendarUtil
import com.santtuhyvarinen.habittracker.utils.TaskUtil

class TaskManager(private val databaseManager: DatabaseManager) {

    val tasks : MutableLiveData<ArrayList<TaskModel>> by lazy {
        MutableLiveData<ArrayList<TaskModel>>()
    }

    fun generateDailyTasks(habits : List<HabitWithTaskLogs>) {
        val taskList = ArrayList<TaskModel>()

        for(habitWithTaskLogs in habits) {
            if(habitWithTaskLogs.habit.disabled) continue
            
            if(CalendarUtil.isHabitScheduledForToday(habitWithTaskLogs.habit)) {

                //Check if already added a task log for habit today. If already has a task log for today, don't add the task
                if (!TaskUtil.hasTaskLogForToday(habitWithTaskLogs)) {
                    taskList.add(TaskModel(habitWithTaskLogs))
                }
            }
        }

        taskList.sortWith (compareByDescending<TaskModel> { it.habitWithTaskLogs.habit.priority }.thenBy { it.habitWithTaskLogs.habit.name } )

        tasks.value = taskList
    }

    suspend fun insertTaskLog(taskModel: TaskModel, taskStatus : String) {
        val taskLog = TaskLog()

        val habit = taskModel.habitWithTaskLogs.habit

        taskLog.habitId = habit.id
        taskLog.timestamp = System.currentTimeMillis()
        taskLog.status = taskStatus

        when(taskStatus) {
            TaskUtil.STATUS_SUCCESS -> {
                //Increase habit score
                val oldScore = habit.score
                val newScore = oldScore + 1

                habit.score = newScore
                taskLog.score = newScore
            }

            TaskUtil.STATUS_FAILED -> {
                //Reset habit score
                val newScore = 0

                habit.score = newScore
                taskLog.score = newScore
            }
        }


        databaseManager.taskLogRepository.createTaskLog(taskLog)

        databaseManager.habitRepository.updateHabit(habit)
    }
}