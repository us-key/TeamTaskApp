package services.implement;

import constant.Constant;
import controllers.Apps;
import dto.team.EditTeamDto;
import models.Team;
import models.User;
import play.Logger;
import services.TeamService;
import services.UserService;
import util.ConfigUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Created on 2016/05/10.
 */
public class TeamServiceImpl implements TeamService {
	@Override
	public void create(EditTeamDto editTeamDto) {
		Logger.info("TeamServiceImpl#edit");

		UserServiceImpl userService = new UserServiceImpl();

		Team team = new Team();
		team.teamName = editTeamDto.getTeamName();

		// チームメンバー
		// DTOのLISTからユーザーを取得
		// TODO ユーザー名から再度ユーザーを取得して登録ってすごく冗長。。。
		List<String> errorMessages = new ArrayList<String>();
		setMembers(editTeamDto.getMemberListStr(), team.members, errorMessages);

		// 作成ユーザー:ログインユーザーから設定
		team.createUser = userService.findUserByName(Apps.getLoginUserName()).get(0);

		if (errorMessages.size() > 0) {
			// TODO エラーメッセージの返し方
		} else {
			team.save();
		}
	}

	@Override
	public void update(EditTeamDto editTeamDto) {
		Logger.info("TeamServiceImpl#update");
		Team team = Team.find.byId(editTeamDto.getId());
		team.teamName = editTeamDto.getTeamName();

		// チームメンバー
		// 一度クリアした上でDTOからセット
		// TODO ユーザー名から再度ユーザーを取得して登録ってすごく冗長。。
		List<String> errorMessages = new ArrayList<String>();
		team.members.clear();
		setMembers(editTeamDto.getMemberListStr(), team.members, errorMessages);

		if (errorMessages.size() > 0) {
			// TODO エラーメッセージの返し方
		} else {
			team.update();
		}
	}

	/**
	 * ユーザーが所属するチームをユーザー名から取得する.
	 * @param userName
	 * @return
	 */
	@Override
	public List<Team> findTeamListByUserName(String userName) {
		Logger.info("TeamServiceImpl#findTeamListByUserName");

		UserServiceImpl userService = new UserServiceImpl();

		// ユーザー名から該当ユーザーを取得
		User user = userService.findUserByName(userName).get(0);

		// ユーザーの所属チームを取得
		return Team.find.where().eq("members", user).setOrderBy("teamName").findList();

	}

	/**
	 * チーム名でチームを取得する.
	 * @param teamName
	 * @return
	 */
	@Override
	public List<Team> findTeamByName(String teamName) {
		Logger.info("TeamServiceImpl#findTeamByName");
		return Team.find.where().eq("teamName", teamName).findList();
	}

	/**
	 * チームメンバーをセットする.
	 * @param memberListStr
	 * @param members
	 * @param errorMessages
	 */
	private void setMembers(String memberListStr, List<User> members, List<String> errorMessages) {

		UserService userService = new UserServiceImpl();

		for (String userName : memberListStr.split(",")) {
			// ユーザー名からユーザーを取得
			List<User> user = userService.findUserByName(userName);
			// 取得できなかった場合エラー
			if (user.size() == 0) {
				errorMessages.add(userName + ConfigUtil.get(Constant.MSG_E005));
			} else {
				// ユーザー名は重複ない前提
				members.add(user.get(0));
			}
		}
	}

	public List<User> findUserByTeamName(String teamName) {
		Team team = Team.find.where().eq("teamName", teamName).findList().get(0);
		return team.members;
	}

	public Team findTeamById(Long teamId) {
		return Team.find.byId(teamId);
	}
}
