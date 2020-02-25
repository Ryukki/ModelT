/*
 * Copyright (c) 2020 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cec.ccv2hacks.samesite.tomcat;

import de.hybris.platform.core.Registry;
import de.hybris.platform.core.Tenant;
import de.hybris.platform.util.config.ConfigIntf;

import javax.servlet.http.Cookie;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import com.sap.cec.ccv2hacks.samesite.tomcat.SameSiteCookieProcessor.CookieHandler;
import com.sap.cec.ccv2hacks.samesite.tomcat.SameSiteCookieProcessor.LogHandler;


/**
 * Installs application specific SameSite cookie handler and log handler. Since spring beans are usually loaded upon
 * Tomcat startup this is early enough.
 */
public class SameSiteCookieHandlerLoader implements InitializingBean
{

	@Override
	public void afterPropertiesSet() throws Exception
	{
		final Tenant tenant = Registry.getCurrentTenantNoFallback();
		if (tenant.equals(Registry.getMasterTenant()))
		{
			final ConfigIntf cfg = tenant.getConfig();
			if (cfg.getBoolean("cookies.SameSite.install.application.handler", false))
			{
				SameSiteCookieProcessor.setApplicationLogHandler(new ConfigSameSiteLogHandler());
				SameSiteCookieProcessor.setApplicationCookieHandler(new ConfigSameSiteCookieHandler(cfg));
			}
		}
	}

	static class ConfigSameSiteCookieHandler implements CookieHandler
	{
		private final ConfigIntf cfg;

		public ConfigSameSiteCookieHandler(final ConfigIntf cfg)
		{
			this.cfg = cfg;
		}

		@Override
		public String getSameSiteParameter(final Cookie cookie)
		{
			return getSameSiteFor(cookie.getDomain(), cookie.getPath(), cookie.getName());
		}

		protected String getSameSiteFor(final String domain, final String path, final String name)
		{
			String param = cfg.getParameter("cookies." + domain + "." + path + "." + name + ".SameSite");
			if (StringUtils.isBlank(param))
			{
				param = cfg.getParameter("cookies." + domain + "." + path + ".SameSite");
			}
			if (StringUtils.isBlank(param))
			{
				param = cfg.getParameter("cookies." + domain + ".SameSite");
			}
			if (StringUtils.isBlank(param))
			{
				param = cfg.getParameter("cookies.SameSite");
			}
			return StringUtils.isBlank(param) ? "None" : param;
		}
	}

	static class ConfigSameSiteLogHandler implements LogHandler
	{
		private static final Logger _LOG = LoggerFactory.getLogger("cookies.samesite");

		@Override
		public void debug(final String msg)
		{
			_LOG.debug(msg);
		}

		@Override
		public void info(final String msg)
		{
			_LOG.info(msg);
		}

		@Override
		public void warn(final String msg)
		{
			_LOG.warn(msg);
		}

		@Override
		public void error(final String msg, final Exception e)
		{
			if (e != null)
			{
				_LOG.error(msg, e);
			}
			else
			{
				_LOG.error(msg);
			}
		}
	}
}
