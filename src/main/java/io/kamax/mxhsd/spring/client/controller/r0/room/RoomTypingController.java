/*
 * mxhsd - Corporate Matrix Homeserver
 * Copyright (C) 2017 Maxime Dor
 *
 * https://www.kamax.io/
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.kamax.mxhsd.spring.client.controller.r0.room;

import io.kamax.mxhsd.api.IHomeServer;
import io.kamax.mxhsd.spring.client.controller.r0.ClientAPIr0;
import io.kamax.mxhsd.spring.common.controller.EmptyJsonResponse;
import io.kamax.mxhsd.spring.common.controller.JsonController;
import io.kamax.mxhsd.spring.common.service.HomeserverService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

import static org.springframework.web.bind.annotation.RequestMethod.PUT;

@RestController
@RequestMapping(path = ClientAPIr0.Room + "/typing", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public class RoomTypingController extends JsonController {

    private final Logger logger = LoggerFactory.getLogger(RoomTypingController.class);

    private IHomeServer hs;

    @Autowired
    public RoomTypingController(HomeserverService svc) {
        this.hs = svc.get();
    }

    // So Riot stops throwing errors
    // TODO implement
    @RequestMapping(method = PUT, path = "/{mxId:.+}")
    public String inject(
            HttpServletRequest req,
            @PathVariable String roomId,
            @PathVariable String mxId
    ) {
        log(logger, req);
        getAccessToken(req);

        return EmptyJsonResponse.stringify();
    }

}
