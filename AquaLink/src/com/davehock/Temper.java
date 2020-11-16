package com.davehock;

import java.io.IOException;
import java.util.List;

import purejavahidapi.HidDevice;
import purejavahidapi.HidDeviceInfo;
import purejavahidapi.InputReportListener;
import purejavahidapi.PureJavaHidApi;

public class Temper {

	static final int VENDOR_ID = 3141;
	static final int PRODUCT_ID = 29697;
	static final int BUFSIZE = 2048;

	public static void main(String[] args) {
		System.out.println("scanning");
		HidDeviceInfo devInfo = null;
		List<HidDeviceInfo> devList = PureJavaHidApi.enumerateDevices();
		for (HidDeviceInfo info : devList) {
			System.out.println(info.getProductId() + " " + info.getVendorId());
			// if (info.getVendorId() == (short) 0x16C0 &&
			// info.getProductId() == (short) 0x05DF) {
			// if (info.getVendorId() == (short) 0x16C0 && info.getProductId() == (short)
			// 0x0a99) {
			if (info.getVendorId() == (short) VENDOR_ID && info.getProductId() == (short) PRODUCT_ID) {
				devInfo = info;
				readDevice(devInfo);

				break;
			}
		}
	}

	static float rawToCelcius(int rawtemp) {
		float temp_c = rawtemp * (125.f / 32000.f);
		return temp_c;
	}

	static float convertCelciusTo(float deg_c, char unit) {
		if (unit == 'F')
			return (deg_c * 1.8f) + 32.f;
		else if (unit == 'K')
			return (deg_c + 273.15f);
		else
			return deg_c;
	}

	private static void readDevice(HidDeviceInfo devInfo) {
		final HidDevice dev;
		try {
			dev = PureJavaHidApi.openDevice(devInfo);
			byte[] temp = new byte[] { (byte) 0x01, (byte) 0x80, (byte) 0x33, (byte) 0x01, (byte) 0x00, (byte) 0x00,
					(byte) 0x00, (byte) 0x00 };
			dev.setOutputReport((byte) 0x00, temp, temp.length);
			dev.setInputReportListener(new InputReportListener() {
				@Override
				public void onInputReport(HidDevice source, byte Id, byte[] data, int len) {
					System.out.printf("onInputReport: id %d len %d data ", Id, len);
					for (int i = 0; i < len; i++)
						System.out.printf("%02X ", data[i]);
					System.out.println();
					int rawtemp = (data[3] & (byte) 0xFF) + (data[2] << 8);
					if ((data[2] & 0x80) != 0) {
						/* return the negative of magnitude of the temperature */
						rawtemp = -((rawtemp ^ 0xffff) + 1);
					}
					System.out.println("temp = " + convertCelciusTo(rawToCelcius(rawtemp), 'F'));
				}
			});

//			while (true) {
//
//			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
