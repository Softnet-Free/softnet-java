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

public class Fnv1a
{
	public static int get32BitHash(byte[] data)
	{
		return get32BitHash(data, 0, data.length);
	}

	public static int get32BitHash(byte[] data, int offset)
	{
		return get32BitHash(data, offset, data.length - offset);
	}
	
	public static int get32BitHash(byte[] data, int offset, int size)
	{
		final int p = 16777619;
        int hash = -2128831035; // 0x811C9DC5;

        for (int i = offset; i < (offset + size); i++)
            hash = (hash ^ (data[i] & 0xff)) * p;
        
        return hash;
	}
}
