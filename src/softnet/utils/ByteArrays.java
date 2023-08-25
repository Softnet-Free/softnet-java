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

package softnet.utils;

public class ByteArrays
{
	public static boolean equals(byte[] left, int left_offset, byte[] right, int right_offset, int size)
	{
        for (int i = 0; i < size; i++)
        {
            if (left[left_offset + i] != right[right_offset + i])
                return false;
        }
        return true;
	}
}
