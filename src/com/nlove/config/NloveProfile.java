package com.nlove.config;

public class NloveProfile {
	enum Gender {
		m, f, other
	}

	private int yearOfBirth;
	private String username;
	private Gender gender;
	private String about;

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public int getYearOfBirth() {
		return yearOfBirth;
	}

	public void setYearOfBirth(int yearOfBirth) {
		this.yearOfBirth = yearOfBirth;
	}

	public Gender getGender() {
		return gender;
	}

	public void setGender(Gender gender) {
		this.gender = gender;
	}

	public String getAbout() {
		return about;
	}

	public void setAbout(String about) {
		this.about = about;
	}

	public String toString() {
		return String.format("[ Gender: %s, Year of birth: %d, About text: \"%s\" ]", this.gender.toString(), this.yearOfBirth, this.about);
	}

}
