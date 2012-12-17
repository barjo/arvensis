package org.ow2.chameleon.rose.pubsubhubbub.distributedhub.internal;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.ow2.chameleon.rose.pubsubhubbub.distributedhub.HubBackups;

public class HubBackupsImpl implements HubBackups {
	private Set<HubInformations> connectedHubs;

	public HubBackupsImpl() {
		connectedHubs = new HashSet<HubBackupsImpl.HubInformations>();
	}

	public void addConnectedHub(String machineID, String link) {
		connectedHubs.add(new HubInformations(machineID, link));
	}

	public void removeConnectedHubByID(String machineID) {
		for (HubInformations hub : connectedHubs) {
			if (hub.getMachineID().equals(machineID)) {
				connectedHubs.remove(hub);
				return;
			}
		}
	}
	
	public void removeConnectedHubByURI(String URI) {
		for (HubInformations hub : connectedHubs) {
			if (hub.getLink().equals(URI)) {
				connectedHubs.remove(hub);
				return;
			}
		}
	}

	public Set<String> getAllHubsLink() {
		return this.getAllHubsLink(null);
	}

	public Set<String> getAllHubsLink(String excludeMachineID) {
		Set<String> connecteHubslinks = new HashSet<String>();
		for (HubInformations hub : connectedHubs) {
			if (!hub.getMachineID().equals(excludeMachineID)) {
				connecteHubslinks.add(hub.getLink());
			}

		}
		return connecteHubslinks;

	}

	public void addSubscriber(String hubMachineID, String susbcriberMachineID,
			String callbackUrl) {
		for (HubInformations hub : connectedHubs) {
			if (hub.getMachineID().equals(hubMachineID)) {
				hub.addSubscriber(susbcriberMachineID, callbackUrl);
				return;
			}
		}
	}

	public void removeSubscriber(String hubMachineID, String machineID) {
		for (HubInformations hub : connectedHubs) {
			if (hub.getMachineID().equals(hubMachineID)) {
				hub.removeSubscriber(machineID);
				return;
			}
		}
	}

	public Map<String, Collection<String>> getSubscribers() {
		Map<String, Collection<String>> allSubscribers = new HashMap<String, Collection<String>>();
		for (HubInformations hub : connectedHubs) {
			allSubscribers.put(hub.getMachineID(), hub.getSubscribers()
					.values());
		}
		return allSubscribers;
	}

	public void addPublisher(String hubMachineID, String susbcriberMachineID,
			String callbackUrl) {
		for (HubInformations hub : connectedHubs) {
			if (hub.getMachineID().equals(hubMachineID)) {
				hub.addPublisher(susbcriberMachineID, callbackUrl);
				return;
			}
		}
	}

	public void removePublisher(String hubMachineID, String machineID) {
		for (HubInformations hub : connectedHubs) {
			if (hub.getMachineID().equals(hubMachineID)) {
				hub.removePublisher(machineID);
				return;
			}
		}
	}

	public Map<String, Collection<String>> getPublishers() {
		Map<String, Collection<String>> allPublishers = new HashMap<String, Collection<String>>();
		for (HubInformations hub : connectedHubs) {
			allPublishers.put(hub.getMachineID(), hub.getPublishers().values());
		}
		return allPublishers;
	}

	public Map<String, String> getPublishers(String link) {
		for (HubInformations hub : connectedHubs) {
			if (hub.link.equals(link)) {
				return hub.getPublishers();
			}
		}
		return null;
	}

	public Map<String, String> getSubscribers(String link) {
		for (HubInformations hub : connectedHubs) {
			return hub.getSubscribers();
		}
		return null;
	}

	private static class HubInformations {
		private String link;
		private String hubMachineID;
		private Map<String, String> connectedSubscribers;
		private Map<String, String> connectedPublishers;

		public HubInformations(String pMachineId, String pLink) {
			hubMachineID = pMachineId;
			link = pLink;
			// subscriber machineId and callbackURL
			connectedSubscribers = new HashMap<String, String>();
			// publisher machineId and callbackURL
			connectedPublishers = new HashMap<String, String>();
		}

		void removeSubscriber(String machineID) {
			connectedSubscribers.remove(machineID);

		}

		void addSubscriber(String machineId, String callBackUrl) {
			connectedSubscribers.put(machineId, callBackUrl);
		}

		Map<String, String> getSubscribers() {
			return connectedSubscribers;
		}

		void removePublisher(String machineID) {
			connectedPublishers.remove(machineID);

		}

		void addPublisher(String machineId, String callBackUrl) {
			connectedPublishers.put(machineId, callBackUrl);
		}

		public Map<String, String> getPublishers() {
			return connectedPublishers;
		}

		String getLink() {
			return link;
		}

		String getMachineID() {
			return hubMachineID;
		}
	}

}
