package dev.jms.app.user;

import dev.jms.app.user.handler.AccountHandler;
import dev.jms.app.user.handler.AuthHandler;
import dev.jms.app.user.handler.ProfileHandler;
import dev.jms.util.Config;
import dev.jms.util.HttpMethod;
import dev.jms.util.Router;

/** Registra le rotte del modulo user. */
public class Routes
{
  /** Registra le rotte /api/user/*. */
  public static void register(Router router, Config config)
  {
    AccountHandler account = new AccountHandler();
    AuthHandler    auth    = new AuthHandler(config);
    ProfileHandler profile = new ProfileHandler();

    // Account — /sid prima di /{id} (match esatto ha precedenza nel PathTemplateMatcher)
    router.route(HttpMethod.GET,    "/api/user/accounts/sid",  account::sid);
    router.route(HttpMethod.PUT,    "/api/user/accounts/sid",  account::update);
    router.route(HttpMethod.DELETE, "/api/user/accounts/sid",  account::delete);
    router.route(HttpMethod.GET,    "/api/user/accounts/{id}", account::byId);
    router.route(HttpMethod.GET,    "/api/user/accounts",      account::list);
    router.route(HttpMethod.POST,   "/api/user/accounts",      account::register);

    // Auth
    router.route(HttpMethod.GET,    "/api/user/auth/session",           auth::session);
    router.route(HttpMethod.GET,    "/api/user/auth/generate-password",  auth::generatePassword);
    router.route(HttpMethod.POST,   "/api/user/auth/login",             auth::login);
    router.route(HttpMethod.POST,   "/api/user/auth/logout",            auth::logout);
    router.route(HttpMethod.POST,   "/api/user/auth/refresh",           auth::refresh);
    router.route(HttpMethod.POST,   "/api/user/auth/2fa",               auth::twoFactor);
    router.route(HttpMethod.POST,   "/api/user/auth/forgot-password",   auth::forgotPassword);
    router.route(HttpMethod.POST,   "/api/user/auth/reset-password",    auth::resetPassword);
    router.route(HttpMethod.PUT,    "/api/user/auth/change-password",   auth::changePassword);

    // Profile — /sid/settings/{key} prima di /sid/settings prima di /sid
    router.route(HttpMethod.GET,    "/api/user/users/sid/settings/{key}", profile::settingByKey);
    router.route(HttpMethod.DELETE, "/api/user/users/sid/settings/{key}", profile::deleteSetting);
    router.route(HttpMethod.GET,    "/api/user/users/sid/settings",       profile::settings);
    router.route(HttpMethod.POST,   "/api/user/users/sid/settings",       profile::addSetting);
    router.route(HttpMethod.GET,    "/api/user/users/sid",                profile::sid);
    router.route(HttpMethod.PUT,    "/api/user/users/sid",                profile::update);
  }
}
