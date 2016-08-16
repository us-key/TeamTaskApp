package controllers;

import com.google.inject.Inject;
import constant.Constant;
import dto.task.EditTaskMstDto;
import models.TaskMst;
import models.User;
import play.Logger;
import play.data.Form;
import play.mvc.Result;
import play.mvc.Security;
import services.TaskService;
import services.implement.TaskServiceImpl;
import services.implement.TeamServiceImpl;
import util.DateUtil;
import views.html.taskList;
import views.html.taskMst;
import views.html.taskMstList;
import views.html.taskRefer;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created on 2016/05/25.
 */
public class TaskController extends Apps {
	@Inject
	TaskService service;

	/**
	 * タスクリスト画面表示（日付指定あり）.
	 * 渡された日付と利用チームを元に、タスクリストを表示する.
	 * @param dateStr
	 * @return
	 */
	@Security.Authenticated(Secured.class)
	public Result displayTaskListWithDate(String dateStr) {
		Logger.info("TaskController#displayTaskListWithDate"
					+ " dateStr:" + dateStr);
		setSessionUrl(routes.TaskController.displayTaskListWithDate(dateStr).url());

		// セッションのチーム名を取得する
		String teamName = getSessionTeamName();

		// チーム未選択の場合、全件表示にリダイレクトする
		if (Constant.USER_TEAM_BLANK.equals(getSessionTeamName())) {
			return redirect(routes.TaskController.displayTaskList());
		}
		// 未作成のタスクトラン作成
		super.createTaskTrn(teamName, dateStr);

		return ok(taskList.render(dateStr, teamName));
	}

	/**
	 * タスクリスト画面表示.
	 * @return
	 */
	@Security.Authenticated(Secured.class)
	public Result displayTaskList() {
		Logger.info("TaskController#displayTaskList");
		setSessionUrl(routes.TaskController.displayTaskList().url());
		if (Constant.USER_TEAM_BLANK.equals(getSessionTeamName())) {
			super.chkAndCreateTaskTrn(getLoginUserName());
			return ok(taskList.render("", ""));
		} else {
			return redirect(
					routes.TaskController.displayTaskListWithDate(
							DateUtil.getDateStr(new Date(), Constant.DATE_FORMAT_yMd)));
		}
	}

	/**
	 * タスクマスタ新規登録画面表示.
	 * @return
	 */
	@Security.Authenticated(Secured.class)
	public Result displayCreateTaskMst() {
		Logger.info("TaskController#displayCreateTaskMst");
		setSessionUrl(routes.TaskController.displayCreateTaskMst().url());

		EditTaskMstDto dto = new EditTaskMstDto();

		String teamName = getSessionTeamName();
		if (Constant.USER_TEAM_BLANK.equals(teamName)) {
			dto.setTeamName("");
		} else {
			dto.setTeamName(teamName);
		}
		dto.setStartDate(new Date());
		Form<EditTaskMstDto> editTaskMstDtoForm = Form.form(EditTaskMstDto.class);
		return ok(taskMst.render(Constant.MODE_CREATE, editTaskMstDtoForm.fill(dto)));
	}

	/**
	 * タスクマスタ更新画面表示.
	 * @return
	 */
	@Security.Authenticated(Secured.class)
	public Result displayUpdateTaskMst(String teamName, String taskName) {
		Logger.info("TaskController#displayUpdateTaskMst");

		EditTaskMstDto dto = new EditTaskMstDto();

		TaskMst taskMst = service.findTaskMstByTeamAndTaskName(teamName, taskName);

		dto.setId(taskMst.id);
		dto.setMainUserName(taskMst.mainUser.userName);
		List<String> repetition = new ArrayList<String>();
		for (String repStr : taskMst.repetition.split(",")) {
			repetition.add(repStr);
		}
		dto.setRepetition(repetition);
		dto.setRepType(taskMst.repType);
		dto.setStartDate(taskMst.startDate);
		dto.setTaskInfo(taskMst.taskInfo);
		dto.setTaskName(taskMst.taskName);
		dto.setTeamName(taskMst.taskTeam.teamName);

		Form<EditTaskMstDto> editTaskMstDtoForm = Form.form(EditTaskMstDto.class);
		return ok(views.html.taskMst.render(Constant.MODE_UPDATE, editTaskMstDtoForm.fill(dto)));
	}

	/**
	 * タスクマスタ一覧画面表示.
	 * @return
	 */
	@Security.Authenticated(Secured.class)
	public Result displayTaskMstList() {
		Logger.info("TaskController#displayTaskMstList");

		String userName = session("userName");
		String teamName = (session("teamName") == null ? "" : session("teamName"));

		return ok(taskMstList.render(userName, teamName));
	}

	/**
	 * タスクマスタ登録/更新.
	 * @return
	 */
	@Security.Authenticated(Secured.class)
	public Result edit(String mode) {
		Logger.info("TaskController#edit MODE:" + mode);

		Form<EditTaskMstDto> editTaskMstDtoForm = Form.form(EditTaskMstDto.class).bindFromRequest();
		if (!editTaskMstDtoForm.hasErrors()) {
			String msg = "";
			EditTaskMstDto dto = editTaskMstDtoForm.get();
			switch (mode) {
				case Constant.MODE_CREATE :
					service.createTaskMst(dto);
					flashSuccess(Constant.MSG_I003);
					break;
				case Constant.MODE_UPDATE :
					service.updateTaskMst(dto);
					flashSuccess(Constant.MSG_I004);
					break;
			}
			// タスクリストに遷移
			return redirect(routes.TaskController.displayTaskList());

		} else {
			flashError(Constant.MSG_E003);
			return badRequest(taskMst.render(mode, editTaskMstDtoForm));
		}

	}

	/**
	 * タスク参照画面を表示する.
	 * @param teamName
	 * @param taskName
	 * @return
	 */
	@Security.Authenticated(Secured.class)
	public Result referTask(String teamName, String taskName) {
		return ok(taskRefer.render(teamName, taskName));
	}

	public static TaskMst findTaskMstByTeamAndTaskName(String teamName, String taskName) {
		TaskServiceImpl service = new TaskServiceImpl();
		return service.findTaskMstByTeamAndTaskName(teamName, taskName);
	}

	/**
	 * タスク参照画面に表示するユーザーごとのタスク実施回数をHTMLにする.
	 * @param teamName
	 * @param taskName
	 * @return
	 */
	public static String editTaskDoneCountHtml(String teamName, String taskName) {
		Logger.info("TaskController#editTaskDoneCountHtml");

		TaskServiceImpl service = new TaskServiceImpl();
		TeamServiceImpl teamService = new TeamServiceImpl();

		String html = "";

		// フォーマット：[ユーザー名] : N回
		String htmlFormat = "<p>[userName] : NNN 回</p>";

		// チームに所属するユーザー
		List<User> userList = teamService.findUserByTeamName(teamName);

		for (User user : userList) {
			Integer count = 0;
			String userName = user.userName;
			if ((count = service.getTaskDoneCount(
					userName, teamName, taskName)) > 0) {
				String addHtml = htmlFormat;
				addHtml = addHtml.replace("[userName]", userName)
								 .replace("NNN", count.toString());
				html += addHtml;
			}

		}
		return html;
	}

	public static String getTaskRepetition(String teamName, String taskName) {
		TaskServiceImpl service = new TaskServiceImpl();

		TaskMst taskMst = service.findTaskMstByTeamAndTaskName(teamName, taskName);
		String repTypeStr = getRepTypeStr(taskMst.repType);

		return repTypeStr
				+ ("".equals(taskMst.repetition) ? "" : "/" + taskMst.repetition);
	}

	private static String getRepTypeStr(String repType) {
		switch (repType) {
			case Constant.REPTYPE_DAYLY :
				return Constant.REPTYPE_STR_DAYLY;
			case Constant.REPTYPE_WEEKLY :
				return Constant.REPTYPE_STR_WEEKLY;
			case Constant.REPTYPE_MONTHLY :
				return Constant.REPTYPE_MONTHLY;
			default :
				return null;
		}
	}

}
