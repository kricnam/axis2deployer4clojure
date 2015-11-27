package com.gubnoi.Axis2Extender;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;

import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.deployment.AbstractDeployer;
import org.apache.axis2.deployment.DeploymentEngine;
import org.apache.axis2.deployment.DeploymentErrorMsgs;
import org.apache.axis2.deployment.DeploymentException;
import org.apache.axis2.deployment.repository.util.ArchiveReader;
import org.apache.axis2.deployment.repository.util.DeploymentFileData;
import org.apache.axis2.deployment.util.Utils;
import org.apache.axis2.description.AxisOperation;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.description.AxisServiceGroup;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.axis2.engine.ServiceLifeCycle;
import org.apache.axis2.i18n.Messages;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ClojureDeployer extends AbstractDeployer {
	private static final Log log = LogFactory.getLog(ClojureDeployer.class);

	private ClassLoader cl;

	private AxisConfiguration axisConfig;

	private ConfigurationContext configCtx;

	private String directory;

	// To initialize the deployer
	public void init(ConfigurationContext configCtx) {
		this.configCtx = configCtx;
		this.axisConfig = this.configCtx.getAxisConfiguration();

		cl = configCtx.getAxisConfiguration().getServiceClassLoader();
		log.info(cl.toString());

		// Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());

	}

	protected void printClassLoader() {
		try {
			Enumeration<URL> e;
			e = cl.getResources("");
			while (e.hasMoreElements()) {
				log.info("ClassLoader Resource: " + e.nextElement());
			}
			log.info("Class Resource: " + cl.getResource("/"));

		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}

	// Will process the file and add that to axisConfig
	public void deploy(DeploymentFileData deploymentFileData) throws DeploymentException {

		log.info(deploymentFileData.getAbsolutePath());

		log.info("before:" + Thread.currentThread().getContextClassLoader().toString());

		log.info(cl.getClass().getProtectionDomain().getCodeSource().getLocation().toString());

		// cl =
		// Utils.getClassLoader(cl,"/home/mxx/Downloads/axis2-1.6.3/repository/aar/svctest-0.1.0-standalone21.aar",true);
		cl = this.getClass().getClassLoader();
		try {
			Enumeration<URL> e;
			e = cl.getResources("");
			while (e.hasMoreElements()) {
				log.info("ClassLoader Resource: " + e.nextElement());
			}
			log.info("Class Resource: " + cl.getResource("/"));
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		Thread.currentThread().setContextClassLoader(cl);
		log.info("after:" + Thread.currentThread().getContextClassLoader().toString());
		log.info("deploy starting...");
		/////////////////////////////////////////////////////////////
		boolean isDirectory = deploymentFileData.getFile().isDirectory();
		ArchiveReader archiveReader;
		StringWriter errorWriter = new StringWriter();
		archiveReader = new ArchiveReader();
		String serviceStatus = "";
		try {
			deploymentFileData.setClassLoader(isDirectory, axisConfig.getServiceClassLoader(),
					(File) axisConfig.getParameterValue(Constants.Configuration.ARTIFACTS_TEMP_DIR),
					axisConfig.isChildFirstClassLoading());
			HashMap<String, AxisService> wsdlservice = archiveReader.processWSDLs(deploymentFileData);
			if (wsdlservice != null && wsdlservice.size() > 0) {
				for (AxisService service : wsdlservice.values()) {
					Iterator<AxisOperation> operations = service.getOperations();
					while (operations.hasNext()) {
						AxisOperation axisOperation = operations.next();
						axisConfig.getPhasesInfo().setOperationPhases(axisOperation);
					}
				}
			}
			AxisServiceGroup serviceGroup = new AxisServiceGroup(axisConfig);
			serviceGroup.setServiceGroupClassLoader(deploymentFileData.getClassLoader());
			
			Thread.currentThread().setContextClassLoader(serviceGroup.getServiceGroupClassLoader());
			log.info("switch class loader");
			cl = serviceGroup.getServiceGroupClassLoader();
			printClassLoader();
					
			ArrayList<AxisService> serviceList = archiveReader.processServiceGroup(deploymentFileData.getAbsolutePath(),
					deploymentFileData, serviceGroup, isDirectory, wsdlservice, configCtx);
			URL location = deploymentFileData.getFile().toURL();

			// Add the hierarchical path to the service group
			if (location != null) {
				String serviceHierarchy = Utils.getServiceHierarchy(location.getPath(), this.directory);
				if (!"".equals(serviceHierarchy)) {
					serviceGroup.setServiceGroupName(serviceHierarchy + serviceGroup.getServiceGroupName());
					for (AxisService axisService : serviceList) {
						axisService.setName(serviceHierarchy + axisService.getName());
					}
				}
			}
			DeploymentEngine.addServiceGroup(serviceGroup, serviceList, location, deploymentFileData, axisConfig);

			super.deploy(deploymentFileData);
		} catch (DeploymentException de) {
			de.printStackTrace();
			log.error(Messages.getMessage(DeploymentErrorMsgs.INVALID_SERVICE, deploymentFileData.getName(),
					de.getMessage()), de);
			PrintWriter error_ptintWriter = new PrintWriter(errorWriter);
			de.printStackTrace(error_ptintWriter);
			serviceStatus = "Error:\n" + errorWriter.toString();

			throw de;

		} catch (AxisFault axisFault) {
			log.error(Messages.getMessage(DeploymentErrorMsgs.INVALID_SERVICE, deploymentFileData.getName(),
					axisFault.getMessage()), axisFault);
			PrintWriter error_ptintWriter = new PrintWriter(errorWriter);
			axisFault.printStackTrace(error_ptintWriter);
			serviceStatus = "Error:\n" + errorWriter.toString();

			throw new DeploymentException(axisFault);

		} catch (Exception e) {
			if (log.isInfoEnabled()) {
				StringWriter sw = new StringWriter();
				PrintWriter pw = new PrintWriter(sw);
				e.printStackTrace(pw);
				log.info(Messages.getMessage(DeploymentErrorMsgs.INVALID_SERVICE, deploymentFileData.getName(),
						sw.getBuffer().toString()));
			}
			PrintWriter error_ptintWriter = new PrintWriter(errorWriter);
			e.printStackTrace(error_ptintWriter);
			serviceStatus = "Error:\n" + errorWriter.toString();

			throw new DeploymentException(e);

		} catch (Throwable t) {
			if (log.isInfoEnabled()) {
				StringWriter sw = new StringWriter();
				PrintWriter pw = new PrintWriter(sw);
				t.printStackTrace(pw);
				log.info(Messages.getMessage(DeploymentErrorMsgs.INVALID_SERVICE, deploymentFileData.getName(),
						sw.getBuffer().toString()));
			}
			PrintWriter error_ptintWriter = new PrintWriter(errorWriter);
			t.printStackTrace(error_ptintWriter);
			serviceStatus = "Error:\n" + errorWriter.toString();

			throw new DeploymentException(new Exception(t));

		} finally {
			if (serviceStatus.startsWith("Error:")) {
				axisConfig.getFaultyServices().put(deploymentFileData.getFile().getAbsolutePath(), serviceStatus);
			}
		}

	}

	public void setDirectory(String directory) {
		this.directory = directory;
	}

	public void setExtension(String extension) {
	}

	public void undeploy(String fileName) throws DeploymentException {
		try {
			// find the hierarchical part of the service group name
			String serviceHierarchy = Utils.getServiceHierarchy(fileName, this.directory);
			fileName = Utils.getShortFileName(fileName);
			fileName = DeploymentEngine.getAxisServiceName(fileName);

			// attach the hierarchical part if it is not null
			if (serviceHierarchy != null) {
				fileName = serviceHierarchy + fileName;
			}
			AxisServiceGroup serviceGroup = axisConfig.removeServiceGroup(fileName);
			// Fixed - https://issues.apache.org/jira/browse/AXIS2-4610
			if (serviceGroup != null) {
				for (Iterator services = serviceGroup.getServices(); services.hasNext();) {
					AxisService axisService = (AxisService) services.next();
					ServiceLifeCycle serviceLifeCycle = axisService.getServiceLifeCycle();
					if (serviceLifeCycle != null) {
						serviceLifeCycle.shutDown(configCtx, axisService);
					}
				}
				configCtx.removeServiceGroupContext(serviceGroup);
				log.info(Messages.getMessage(DeploymentErrorMsgs.SERVICE_REMOVED, fileName));
			} else {
				axisConfig.removeFaultyService(fileName);
			}
			super.undeploy(fileName);
		} catch (AxisFault axisFault) {
			// May be a faulty service
			axisConfig.removeFaultyService(fileName);

			throw new DeploymentException(axisFault);
		}
	}

}
