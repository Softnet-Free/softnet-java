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

public final class ErrorCodes 
{
	public final static int SECURITY_ERROR = 4;
    public final static int UNSUPPORTED_HOST_FUNCTIONALITY = 5;
    public final static int PERSISTENCE_ERROR = 6;
    public final static int NO_RESPONSE = 32;
	public final static int NETWORK_ERROR = 34;
	public final static int INPUT_DATA_FORMAT_ERROR = 38;
	public final static int INPUT_DATA_INCONSISTENT = 39;
	public final static int ILLEGAL_NAME = 52;
	public final static int UNEXPECTED_ERROR = 63;

    public final static int INCOMPATIBLE_PROTOCOL_VERSION = 92;
	public final static int INVALID_SERVER_ENDPOINT = 93;

	public final static int SERVICE_NOT_REGISTERED = 95;
	public final static int INVALID_CLIENT_CATEGORY = 96;
	public final static int CLIENT_NOT_REGISTERED = 97;
	public final static int PASSWORD_NOT_MATCHED = 98;
    public final static int DUPLICATED_SERVICE_UID_USAGE = 100;
    public final static int DUPLICATED_CLIENT_KEY_USAGE = 101;
    public final static int CONSTRAINT_VIOLATION = 102;
    
	public final static int CLIENT_OFFLINE = 105;

    public final static int ENDPOINT_DATA_FORMAT_ERROR = 110;
	public final static int ENDPOINT_DATA_INCONSISTENT = 111;
	
	public final static int ACCESS_DENIED = 112;
	public final static int SERVICE_OFFLINE = 115;
	public final static int SERVICE_BUSY = 116;
	
	public final static int PORT_UNREACHABLE = 121;
	public final static int CONNECTION_ATTEMPT_FAILED = 122;
	
	public final static int MISSING_PROCEDURE = 132;
	public final static int ARGUMENT_ERROR = 133;
	
	public final static int SERVER_BUSY = 232;
	public final static int SERVER_CONFIG_ERROR = 233;	
	public final static int SERVER_DBMS_ERROR = 234;	
	public final static int SERVER_DATA_INTEGRITY_ERROR = 238;

	public final static int RESTART_DEMANDED = 245;
	public final static int TIMEOUT_EXPIRED = 250;
}