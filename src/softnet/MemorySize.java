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

package softnet;

public class MemorySize {
	public static int fromK(int kilobytes) {
		return kilobytes * 1024;
	}
	public static int fromM(int megabytes) {
		return megabytes * 1048576;
	}
	public static long fromG(int gigabytes) {
		return gigabytes * 1073741824L;
	}
	public static int fromMK(int megabytes, int kilobytes) {
		return megabytes * 1048576 + kilobytes * 1024;
	}
	public static long fromMK_L(int megabytes, int kilobytes) {
		return megabytes * 1048576L + kilobytes * 1024L;
	}
	public static int fromGMK(int gigabytes, int megabytes, int kilobytes) {
		return gigabytes * 1073741824 +  megabytes * 1048576 + kilobytes * 1024;
	}
	public static long fromGMK_L(int gigabytes, int megabytes, int kilobytes) {
		return gigabytes * 1073741824L +  megabytes * 1048576L + kilobytes * 1024L;
	}
}
