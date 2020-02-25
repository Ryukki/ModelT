/*
 * [y] hybris Platform
 *
 * Copyright (c) 2000-2020 SAP SE
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of SAP
 * Hybris ("Confidential Information"). You shall not disclose such
 * Confidential Information and shall use it only in accordance with the
 * terms of the license agreement you entered into with SAP Hybris.
 */
package com.sap.cec.ccv2hacks.samesite.tomcat;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.http.Cookie;

import org.apache.tomcat.util.http.Rfc6265CookieProcessor;

/**
 *
 */
public class SameSiteCookieProcessor extends Rfc6265CookieProcessor
{
	public interface CookieHandler
	{
		String getSameSiteParameter(Cookie cookie);
	}

	public interface LogHandler
	{
		void debug(String msg);

		void info(String msg);

		void warn(String msg);

		void error(String msg, Exception e);
	}

	private static volatile CookieHandler applicationCookieHandler = new CookieHandler()
	{

		private final Properties props = loadSameSiteProperties();
		private final Map<Object, String> cache = new ConcurrentHashMap<Object, String>();

		@Override
		public String getSameSiteParameter(final Cookie cookie)
		{
			return getSameSiteFor(cookie.getDomain(), cookie.getPath(), cookie.getName());
		}

		String getSameSiteFor(final String domain, final String path, final String name)
		{
			final Object key = keyFor(domain, path, name);
			String param = cache.get(key);
			if (param == null)
			{
				param = props.getProperty("cookies." + domain + "." + path + "." + name + ".SameSite");
				if (param == null)
				{
					param = props.getProperty("cookies." + domain + "." + path + ".SameSite");
				}
				if (param == null)
				{
					param = props.getProperty("cookies." + domain + ".SameSite");
				}
				if (param == null)
				{
					param = props.getProperty("cookies.SameSite");
				}
				if (param == null)
				{
					param = "None";
				}
				System.out.println("SameSite Cookie Processor: " + key + " -> " + param);
				cache.put(key, param);
			}
			return param;
		}

		Object keyFor(final String domain, final String path, final String name)
		{
			return "Cookie(domain:" + domain + ",path:" + path + ",name:" + name + ")";
		}
	};

	static Properties loadSameSiteProperties()
	{
		final Properties props = new Properties();
		final File cfgFile = new File("sameSiteCookies.properties");
		try
		{
			if (cfgFile.canRead())
			{
				try (final InputStream in = new FileInputStream("sameSiteCookies.properties"))
				{
					props.load(in);

					System.out.println("SameSite Cookie Processor: Loaded settings from " + cfgFile.getCanonicalPath() + ": " + props);
				}
			}
			else
			{
				System.out.println("No Same Site cookie config file found at " + cfgFile.getCanonicalPath());
			}
		}
		catch (final IOException e)
		{
			System.err.println("Error loading from " + cfgFile + ". Msg: " + e);
			e.printStackTrace();
		}
		return props;
	}

	private static volatile LogHandler applicationLogHandler;

	public static void setApplicationCookieHandler(final CookieHandler appCookieHandler)
	{
		debug("Application cookie handler set to " + appCookieHandler);
		applicationCookieHandler = appCookieHandler;
	}

	public static void setApplicationLogHandler(final LogHandler appLogHandler)
	{
		applicationLogHandler = appLogHandler;
		debug("Application log handler set to " + appLogHandler);
	}


	@Override
	public String generateHeader(final Cookie cookie)
	{
		final String generated = super.generateHeader(cookie);

		try
		{
			if (generated.matches("(?i)[ ;]SameSite\\w*="))
			{
				debug("Cookie " + toString(cookie) + " already got SameSite setting");
				return generated;
			}
			else
			{
				final String sameSiteAppSetting = getSameSiteAppSetting(cookie);
				debug("Cookie " + toString(cookie) + " is using SameSite app setting '" + sameSiteAppSetting + "'");
				return generated + "; SameSite=" + sameSiteAppSetting;
			}
		}
		catch (final Exception e)
		{
			error("Error adjusting SameSite for cookie " + toString(cookie) + " setting:" + e.getMessage(), e);
			return generated;
		}
	}

	protected String toString(final Cookie cookie)
	{
		return cookie.getDomain() + ":" + cookie.getPath() + ":" + cookie.getName();
	}

	protected String getSameSiteAppSetting(final Cookie cookie)
	{
		final String appSetting = applicationCookieHandler != null ? applicationCookieHandler.getSameSiteParameter(cookie) : null;
		return appSetting != null ? appSetting : "None";
	}

	protected static void debug(final String msg)
	{
		if (applicationLogHandler != null)
		{
			applicationLogHandler.debug(msg);
		}
	}

	protected static void info(final String msg)
	{
		if (applicationLogHandler != null)
		{
			applicationLogHandler.info(msg);
		}
	}

	protected static void warn(final String msg)
	{
		if (applicationLogHandler != null)
		{
			applicationLogHandler.warn(msg);
		}
	}

	protected static void error(final String msg, final Exception e)
	{
		if (applicationLogHandler != null)
		{
			applicationLogHandler.error(msg, e);
		}
	}
}
