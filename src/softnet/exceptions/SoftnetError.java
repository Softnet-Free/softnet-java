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

package softnet.exceptions;

public enum SoftnetError
{
	NoError(0),
	SecurityError(ErrorCodes.SECURITY_ERROR),
	NoResponse(ErrorCodes.NO_RESPONSE),
	UnsupportedHostFunctionality(ErrorCodes.UNSUPPORTED_HOST_FUNCTIONALITY),
	PersistenceError(ErrorCodes.PERSISTENCE_ERROR),
	NetworkError(ErrorCodes.NETWORK_ERROR),
	IncompatibleProtocolVersion(ErrorCodes.INCOMPATIBLE_PROTOCOL_VERSION),
	InvalidServerEndpoint(ErrorCodes.INVALID_SERVER_ENDPOINT),
	EndpointDataFormatError(ErrorCodes.ENDPOINT_DATA_FORMAT_ERROR),
	EndpointDataInconsistent(ErrorCodes.ENDPOINT_DATA_INCONSISTENT),
	InputDataFormatError(ErrorCodes.INPUT_DATA_FORMAT_ERROR),
	InputDataInconsistent(ErrorCodes.INPUT_DATA_INCONSISTENT),
	IllegalName(ErrorCodes.ILLEGAL_NAME),
	UnexpectedError(ErrorCodes.UNEXPECTED_ERROR),
	ClientOffline(ErrorCodes.CLIENT_OFFLINE),
	AccessDenied(ErrorCodes.ACCESS_DENIED),
	PortUnreachable(ErrorCodes.PORT_UNREACHABLE),
	ServiceOffline(ErrorCodes.SERVICE_OFFLINE),
	ServiceBusy(ErrorCodes.SERVICE_BUSY),
	MissingProcedure(ErrorCodes.MISSING_PROCEDURE),
	ArgumentError(ErrorCodes.ARGUMENT_ERROR),
	ConnectionAttemptFailed(ErrorCodes.CONNECTION_ATTEMPT_FAILED),
	ServiceNotRegistered(ErrorCodes.SERVICE_NOT_REGISTERED),
	InvalidClientCategory(ErrorCodes.INVALID_CLIENT_CATEGORY),
	ClientNotRegistered(ErrorCodes.CLIENT_NOT_REGISTERED),
	PasswordNotMatched(ErrorCodes.PASSWORD_NOT_MATCHED),
	DublicatedServiceUidUsage(ErrorCodes.DUPLICATED_SERVICE_UID_USAGE),
	DublicatedClientKeyUsage(ErrorCodes.DUPLICATED_CLIENT_KEY_USAGE),
	ConstraintViolation(ErrorCodes.CONSTRAINT_VIOLATION),
	TimeoutExpired(ErrorCodes.TIMEOUT_EXPIRED),
	ServerBusy(ErrorCodes.SERVER_BUSY),
	ServerConfigError(ErrorCodes.SERVER_CONFIG_ERROR),
	ServerDbmsError(ErrorCodes.SERVER_DBMS_ERROR),
	ServerDataIntegrityError(ErrorCodes.SERVER_DATA_INTEGRITY_ERROR),
	RestartDemanded(ErrorCodes.RESTART_DEMANDED);
	 	
	public final int Code;
	SoftnetError(int code) { Code = code; }
}
