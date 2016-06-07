package controllers;

import com.google.inject.Inject;
import dto.user.CreateUserDto;
import play.data.Form;
import play.mvc.Result;
import services.UserService;
import views.html.user;

/**
 * Created on 2016/05/08.
 */
public class UserController extends Apps {

	@Inject
	UserService service;

	/**
	 * ユーザー新規登録画面表示.
	 * @return
	 */
	public Result displayCreateUser() {
		Form<CreateUserDto> createUserDtoForm = Form.form(CreateUserDto.class);
		return ok(user.render("CREATE", createUserDtoForm));
	}

	/**
	 * ユーザー新規登録.
	 * @return
	 */
	public Result create() {
		Form<CreateUserDto> createUserDtoForm = Form.form(CreateUserDto.class).bindFromRequest();
		if (!createUserDtoForm.hasErrors()) {
			CreateUserDto dto = createUserDtoForm.get();
			service.create(dto);
			String msg = "登録しました。";
			msg += " userName: " + dto.userName;
			flash("success", msg);
			// TODO ログイン済みの状態にしてチーム一覧に遷移
			session().clear();
			session("userName", dto.userName);
			return redirect(routes.TeamController.displayTeamList());
		} else {
			// TODO
			flash("error", "登録できません。");
			return badRequest(user.render("CREATE", createUserDtoForm));
		}
	}
}
