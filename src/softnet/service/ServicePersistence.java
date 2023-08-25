/*
*	Copyright 2023 Robert Koifman
*
*   Licensed under the Apache License, Version 2.0 (the "License");
*   you may not use this file except in compliance with the License.
*   You may obtain a copy of the License at
*
*   http://www.apache.org/licenses/LICENSE-2.0
*
*   Unless required by applicable law or agreed to in writing, software
*   distributed under the License is distributed on an "AS IS" BASIS,
*   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
*   See the License for the specific language governing permissions and
*   limitations under the License.
*/

package softnet.service;

import java.util.UUID;
import softnet.exceptions.*;

public interface ServicePersistence
{
	UUID getUid();
	void setStorageMode();
	boolean isInCacheMode();
	boolean isInStorageMode();
	void invalidateAncientData() throws PersistenceIOSoftnetException;
	void reset() throws PersistenceIOSoftnetException;
	void clear() throws PersistenceIOSoftnetException;
	void cache(ReplacingEvent event) throws PersistenceIOSoftnetException, PersistenceStorageFullSoftnetException;
	void cache(QueueingEvent event) throws PersistenceIOSoftnetException, PersistenceStorageFullSoftnetException;
	void cache(PrivateEvent event) throws PersistenceIOSoftnetException, PersistenceStorageFullSoftnetException;
	ServiceEventPersistable setAcknowledment(int eventKind, String eventName) throws PersistenceIOSoftnetException, PersistenceDataFormatSoftnetException;
	ServiceEventPersistable peek(int eventKind, String eventName) throws PersistenceIOSoftnetException, PersistenceDataFormatSoftnetException;
	void save(ReplacingEvent event) throws PersistenceIOSoftnetException, PersistenceStorageFullSoftnetException;
	void save(QueueingEvent event) throws PersistenceIOSoftnetException, PersistenceStorageFullSoftnetException;
	void save(PrivateEvent event) throws PersistenceIOSoftnetException, PersistenceStorageFullSoftnetException;
	void setAcknowledment() throws PersistenceIOSoftnetException;
	ServiceEventPersistable peek() throws PersistenceIOSoftnetException, PersistenceDataFormatSoftnetException;
	void close();
}
