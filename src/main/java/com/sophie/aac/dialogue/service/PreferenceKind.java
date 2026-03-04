package com.sophie.aac.dialogue.service;

enum PreferenceKind {
  DRINK,
  FOOD,
  PET,
  TEACHER,
  FAMILY_MEMBER,
  ACTIVITY,
  SUBJECT;

  String value() {
    return name();
  }
}
