package com.geros.backend.policy;

import lombok.Data;

public class ReservedUsernameDTO {
    
    @Data
    public static class Request {
        private String username;
        private String description;
    }

    @Data
    public static class Response {
        private Long id;
        private String username;
        private String description;
        private boolean system;

        public static Response from(ReservedUsername entity) {
            Response r = new Response();
            r.id = entity.getId();
            r.username = entity.getUsername();
            r.description = entity.getDescription();
            r.system = entity.isSystem();
            return r;
        }
    }
}