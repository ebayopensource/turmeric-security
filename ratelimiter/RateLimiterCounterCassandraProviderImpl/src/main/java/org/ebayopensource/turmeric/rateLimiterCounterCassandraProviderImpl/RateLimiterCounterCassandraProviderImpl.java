/*******************************************************************************
 * Copyright (c) 2006-2010 eBay Inc. All Rights Reserved.
 * Licensed under the Apache License, Version 2.0 (the "License"); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at 
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *    
 *******************************************************************************/
package org.ebayopensource.turmeric.rateLimiterCounterCassandraProviderImpl;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.Properties;
import java.util.Set;

import org.ebayopensource.turmeric.rateLimiterCounterCassandraProviderImpl.dao.ActiveEffectDao;
import org.ebayopensource.turmeric.rateLimiterCounterCassandraProviderImpl.dao.ActiveEffectDaoImpl;
import org.ebayopensource.turmeric.rateLimiterCounterCassandraProviderImpl.dao.ActiveRLDao;
import org.ebayopensource.turmeric.rateLimiterCounterCassandraProviderImpl.dao.ActiveRLDaoImpl;
import org.ebayopensource.turmeric.rateLimiterCounterCassandraProviderImpl.model.ActiveEffect;
import org.ebayopensource.turmeric.rateLimiterCounterCassandraProviderImpl.model.ActiveRL;
import org.ebayopensource.turmeric.rateLimiterCounterProvider.RateLimiterCounterProvider;
import org.ebayopensource.turmeric.rateLimiterCounterProvider.Policy.model.RateLimiterPolicyModel;
import org.ebayopensource.turmeric.security.v1.services.RateLimiterStatus;
import org.ebayopensource.turmeric.utils.ContextUtils;

/**
 * The Class RateLimiterCounterCassandraProviderImpl.
 * 
 * @author jamuguerza
 */
public class RateLimiterCounterCassandraProviderImpl implements
		RateLimiterCounterProvider {

	/** The active rl dao. */
	private final ActiveRLDao activeRLDao;
	
	/** The active effect dao. */
	private final ActiveEffectDao activeEffectDao;

	/** The Constant cassandraPropFilePath. */
	private static final String cassandraPropFilePath = "META-INF/config/cassandra/cassandra.properties";
	
	/** The Constant c_hostIp. */
    private static final String c_clusterName = "cassandra-rl-cluster-name";

	/** The Constant c_hostIp. */
	private static final String c_hostIp = "cassandra-host-ip";
	
	/** The Constant c_rpcPort. */
	private static final String c_rpcPort = "cassandra-rpc-port";
	
	/** The Constant c_keyspace. */
	private static final String c_keyspace = "cassandra-rl-keyspace";

	/** The Constant c_activeRL_cf. */
	private static final String c_activeRL_cf = "cassandra-active-rl-column-family";
	
	/** The Constant c_activeEffect_cf. */
	private static final String c_activeEffect_cf = "cassandra-active-effect-column-family";

	/** The host. */
	private static String host;
	
	/** The keyspace. */
	private static String keyspace;
	
	/** The active rlcf. */
	private static String activeRLCF;
	
	/** The active effect cf. */
	private static String activeEffectCF;

    private static String clusterName;

	{
		getCassandraConfig();
	}

	/**
	 * Instantiates a new rate limiter counter cassandra provider impl.
	 */
	public RateLimiterCounterCassandraProviderImpl() {
		activeRLDao = new ActiveRLDaoImpl(clusterName, host, keyspace, activeRLCF);
		activeEffectDao = new ActiveEffectDaoImpl(clusterName, host, keyspace,
				activeEffectCF);
	}

	/**
	 * Gets the cassandra config.
	 *
	 * @return the cassandra config
	 */
	private static void getCassandraConfig() {
		ClassLoader classLoader = ContextUtils.getClassLoader();
		InputStream inStream = classLoader
				.getResourceAsStream(cassandraPropFilePath);

		if (inStream != null) {
			Properties properties = new Properties();
			try {
				properties.load(inStream);
				clusterName = properties.getProperty(c_clusterName);
				host = (String) properties.get(c_hostIp) + ":"
						+ (String) properties.get(c_rpcPort);

				keyspace = (String) properties.get(c_keyspace);

				activeRLCF = (String) properties.get(c_activeRL_cf);
				activeEffectCF = (String) properties.get(c_activeEffect_cf);

			} catch (IOException e) {
				// ignore
			} finally {
				try {
					inStream.close();
				} catch (IOException e) {
					// ignore
				}
			}
		}

	}

	/* (non-Javadoc)
	 * @see org.ebayopensource.turmeric.rateLimiterCounterProvider.RateLimiterCounterProvider#getActiveRLKeys()
	 */
	public Set<String> getActiveRLKeys() {
		return activeRLDao.getKeys();
	}

	/* (non-Javadoc)
	 * @see org.ebayopensource.turmeric.rateLimiterCounterProvider.RateLimiterCounterProvider#addActiveRL(java.lang.String, org.ebayopensource.turmeric.rateLimiterCounterProvider.Policy.model.RateLimiterPolicyModel)
	 */
	public void addActiveRL(final String key,
			final RateLimiterPolicyModel rateLimiterPolicyModel) {
		activeRLDao.save(key, (ActiveRL) rateLimiterPolicyModel);
	}

	/* (non-Javadoc)
	 * @see org.ebayopensource.turmeric.rateLimiterCounterProvider.RateLimiterCounterProvider#cointainKeyInActiveRL(java.lang.String)
	 */
	public boolean cointainKeyInActiveRL(final String key) {
		return activeRLDao.containsKey(key);
	}

	/* (non-Javadoc)
	 * @see org.ebayopensource.turmeric.rateLimiterCounterProvider.RateLimiterCounterProvider#addActiveEffect(java.lang.String, org.ebayopensource.turmeric.rateLimiterCounterProvider.Policy.model.RateLimiterPolicyModel)
	 */
	public void addActiveEffect(final String key,
			final RateLimiterPolicyModel rateLimiterPolicyModel) {
		activeEffectDao.save(key, (ActiveEffect) rateLimiterPolicyModel);
	}

	/* (non-Javadoc)
	 * @see org.ebayopensource.turmeric.rateLimiterCounterProvider.RateLimiterCounterProvider#removeActiveEffect(java.lang.String)
	 */
	public void removeActiveEffect(final String key) {
		activeEffectDao.delete(key);
	}

	/* (non-Javadoc)
	 * @see org.ebayopensource.turmeric.rateLimiterCounterProvider.RateLimiterCounterProvider#incrementRLCounter(java.lang.String)
	 */
	public void incrementRLCounter(final String key) {
		ActiveRL activeRL = activeRLDao.find(key);
		if (activeRL != null) {
			activeRL.setCount(activeRL.getCount() + 1);
			activeRLDao.save(key, activeRL);
		}
	}

	/* (non-Javadoc)
	 * @see org.ebayopensource.turmeric.rateLimiterCounterProvider.RateLimiterCounterProvider#setRLCounter(java.lang.String, int)
	 */
	public void setRLCounter(final String key, int i) {
		ActiveRL activeRL = getOrCreateActiveRL(key);
		activeRL.setCount(i);
		activeRLDao.save(key, activeRL);
	}

	/* (non-Javadoc)
	 * @see org.ebayopensource.turmeric.rateLimiterCounterProvider.RateLimiterCounterProvider#setRLTimestamp(java.lang.String, java.util.Date)
	 */
	public void setRLTimestamp(final String key, final Date date) {
		ActiveRL activeRL = getOrCreateActiveRL(key);
		activeRL.setTimestamp(date);
		activeRLDao.save(key, activeRL);
	}

	/* (non-Javadoc)
	 * @see org.ebayopensource.turmeric.rateLimiterCounterProvider.RateLimiterCounterProvider#setRLActive(java.lang.String, boolean)
	 */
	public void setRLActive(final String key, boolean b) {
		ActiveRL activeRL = getOrCreateActiveRL(key);
		activeRL.setActive(b);
		activeRLDao.save(key, activeRL);
	}

	/* (non-Javadoc)
	 * @see org.ebayopensource.turmeric.rateLimiterCounterProvider.RateLimiterCounterProvider#setRLEffectDuration(java.lang.String, java.lang.Long)
	 */
	public void setRLEffectDuration(final String key, final Long duration) {
		ActiveRL activeRL = getOrCreateActiveRL(key);
		activeRL.setEffectDuration(duration);
		activeRLDao.save(key, activeRL);
	}

	/* (non-Javadoc)
	 * @see org.ebayopensource.turmeric.rateLimiterCounterProvider.RateLimiterCounterProvider#setRLRolloverPeriod(java.lang.String, java.lang.Long)
	 */
	public void setRLRolloverPeriod(final String key, final Long rollover) {
		ActiveRL activeRL = getOrCreateActiveRL(key);
		activeRL.setRolloverPeriod(rollover);
		activeRLDao.save(key, activeRL);
	}

	/* (non-Javadoc)
	 * @see org.ebayopensource.turmeric.rateLimiterCounterProvider.RateLimiterCounterProvider#setRLEffect(java.lang.String, org.ebayopensource.turmeric.security.v1.services.RateLimiterStatus)
	 */
	public void setRLEffect(final String key, final RateLimiterStatus effect) {
		ActiveRL activeRL = getOrCreateActiveRL(key);
		activeRL.setEffect(effect);
		activeRLDao.save(key, activeRL);
	}

	/**
	 * Gets the or create active rl.
	 *
	 * @param key the key
	 * @return the or create active rl
	 */
	private ActiveRL getOrCreateActiveRL(String key) {
		ActiveRL activeRL = activeRLDao.find(key);
		if (activeRL == null) {
			activeRL = new ActiveRL();
		}
		return activeRL;
	}

	/* (non-Javadoc)
	 * @see org.ebayopensource.turmeric.rateLimiterCounterProvider.RateLimiterCounterProvider#getActiveRL(java.lang.String)
	 */
	public RateLimiterPolicyModel getActiveRL(final String key) {
		return activeRLDao.find(key);
	}

	/* (non-Javadoc)
	 * @see org.ebayopensource.turmeric.rateLimiterCounterProvider.RateLimiterCounterProvider#resetEffects()
	 */
	public void resetEffects() {
		Set<String> keys = activeEffectDao.getKeys();
		for (String key : keys) {
			resetEffect(key);
		}
	}

	/**
	 * Reset effect.
	 *
	 * @param currentSubjectOrGroup the current subject or group
	 */
	private void resetEffect(String currentSubjectOrGroup) {

		ActiveEffect activeEffect = activeEffectDao.find(currentSubjectOrGroup);

		// get current date
		java.util.Date date = new java.util.Date();
		if (date.after(new Date(activeEffect.getEffectDuration()))) {
			// remove it
			activeEffectDao.delete(currentSubjectOrGroup);

			if (activeRLDao.containsKey(currentSubjectOrGroup)) {
				activeRLDao.delete(currentSubjectOrGroup);
				activeRLDao.save(currentSubjectOrGroup, new ActiveRL());
			}
		}
	}

}