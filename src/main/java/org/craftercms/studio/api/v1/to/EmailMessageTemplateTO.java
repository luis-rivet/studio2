/*******************************************************************************
 * Crafter Studio Web-content authoring solution
 *     Copyright (C) 2007-2016 Crafter Software Corporation.
 * 
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 * 
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 * 
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.craftercms.studio.api.v1.to;

import java.io.Serializable;

public class EmailMessageTemplateTO implements Serializable {

    private static final long serialVersionUID = 5222897831966329668L;
    /** message title **/
	protected String _subject;
	/** message body **/
	protected String _message;
	

	/**
	 * @return the title
	 */
	public String getSubject() {
		return _subject;
	}

	/**
	 * @param title
	 *            the title to set
	 */
	public void setSubject(final String subject) {
		this._subject = subject;
	}

	/**
	 * @return the body
	 */
	public String getMessage() {
		return _message;
	}

	/**
	 * @param body
	 *            the body to set
	 */
	public void setMessage(final String message) {
		this._message = message;
	}
	
}
