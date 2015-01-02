#!/usr/bin/env python

import argparse
import os
import pebble as libpebble
import subprocess
import sys
import time
import pexpect

MAX_ATTEMPTS = 5

def cmd_ping(pebble, args):
    pebble.ping(cookie=0xDEADBEEF)

def cmd_load(pebble, args):
    pebble.install_app(args.app_bundle, args.nolaunch)

def cmd_load_fw(pebble, args):
    pebble.install_firmware(args.fw_bundle)
    time.sleep(5)
    print 'resetting to apply firmware update...'
    pebble.reset()

def cmd_launch_app(pebble, args):
    pebble.launcher_message(args.app_uuid, "RUNNING")

def cmd_app_msg_send_string(pebble, args):
		pebble.app_message_send_string(args.app_uuid, args.key, args.tuple_string)

def cmd_app_msg_send_uint(pebble, args):
		pebble.app_message_send_uint(args.app_uuid, args.key, args.tuple_uint)

def cmd_app_msg_send_int(pebble, args):
		pebble.app_message_send_int(args.app_uuid, args.key, args.tuple_int)

def cmd_app_msg_send_bytes(pebble, args):
		pebble.app_message_send_byte_array(args.app_uuid, args.key, args.tuple_bytes)

def cmd_remote(pebble, args):
    path=args.odp_file_fullpath
    runodp = args.app_name+" --impress "+path
    pebble.set_nowplaying_metadata("LibreOffice Remote Control ", "Next", "Previous")

    try:
        pexpect.run(runodp, timeout=5)
        window_id = pexpect.run("xdotool search --sync --onlyvisible --class \"libreoffice\"")
	fullscreen = "xdotool key --window " +window_id+" F5"
	pexpect.run(fullscreen) 
    except Exception:
        print "Somethings are going bad"
        return False

    def libreoffice_event_handler(event):
        right_click = "xdotool key --window "+ window_id + "Right"
        left_click = "xdotool key --window "+ window_id + "Left"

        if event == "next":
            pexpect.run(right_click)

        if event == "previous":
            pexpect.run(left_click)

    def music_control_handler(endpoint, resp):
        events = {
            "PLAYPAUSE": "playpause",
            "PREVIOUS": "previous",
            "NEXT": "next"
        }

        libreoffice_event_handler(events[resp])

    print "waiting for events"
    while True:
        try:
            pebble.register_endpoint("MUSIC_CONTROL", music_control_handler)
            time.sleep(5)
        except KeyboardInterrupt:
            return

def cmd_logcat(pebble, args):
    print 'listening for logs...'
    try:
        while True:
            time.sleep(1)
    except KeyboardInterrupt:
        return

def cmd_list_apps(pebble, args):
    apps = pebble.get_appbank_status()
    if apps is not False:
        for app in apps['apps']:
            print '[{}] {}'.format(app['index'], app['name'])
    else:
        print "no apps"

def cmd_rm_app(pebble, args):
    try:
        uuid = args.app_index_or_hex_uuid.decode('hex')
        if len(uuid) == 16:
            pebble.remove_app_by_uuid(uuid, uuid_is_string=False)
            print 'removed app'
            return 0
    except:
        pass
    try:
        idx = int(args.app_index_or_hex_uuid)
        for app in pebble.get_appbank_status()['apps']:
            if app['index'] == idx:
                pebble.remove_app(app["id"], app["index"])
                print 'removed app'
                return 0
    except:
        print 'Invalid arguments. Use bank index or hex app UUID (16 bytes / 32 hex digits)'

def cmd_reinstall_app(pebble, args):
    pebble.reinstall_app(args.app_bundle, args.nolaunch)

def cmd_reset(pebble, args):
    pebble.reset()

def cmd_set_nowplaying_metadata(pebble, args):
    pebble.set_nowplaying_metadata(args.track, args.album, args.artist)

def cmd_notification_email(pebble, args):
    pebble.notification_email(args.sender, args.subject, args.body)

def cmd_notification_sms(pebble, args):
    pebble.notification_sms(args.sender, args.body)

def cmd_get_time(pebble, args):
    print pebble.get_time()

def cmd_set_time(pebble, args):
    pebble.set_time(args.timestamp)

def main():
    parser = argparse.ArgumentParser(description='a utility belt for pebble development')
    parser.add_argument('--pebble_id', type=str, help='the last 4 digits of the target Pebble\'s MAC address. \nNOTE: if \
                        --lightblue is set, providing a full MAC address (ex: "A0:1B:C0:D3:DC:93") won\'t require the pebble \
                        to be discoverable and will be faster')

    parser.add_argument('--lightblue', action="store_true", help='use LightBlue bluetooth API')
    parser.add_argument('--pair', action="store_true", help='pair to the pebble from LightBlue bluetooth API before connecting.')

    subparsers = parser.add_subparsers(help='commands', dest='which')

    remote_parser = subparsers.add_parser('remote', help='control a music app on this PC using Pebble')
    remote_parser.add_argument('app_name', type=str, help='title of application to be controlled')
    remote_parser.add_argument('odp_file_fullpath', type=str, help='full path for libreoffice impress presentation file')
    remote_parser.set_defaults(func=cmd_remote)


    args = parser.parse_args()

    attempts = 0
    while True:
        if attempts > MAX_ATTEMPTS:
            raise 'Could not connect to Pebble'
        try:
            pebble_id = args.pebble_id
            if pebble_id is None and "PEBBLE_ID" in os.environ:
                pebble_id = os.environ["PEBBLE_ID"]
            pebble = libpebble.Pebble(pebble_id, args.lightblue, args.pair)
            break
        except:
            time.sleep(5)
            attempts += 1

    try:
        args.func(pebble, args)
    except Exception as e:
        pebble.disconnect()
        raise e
        return

    pebble.disconnect()

if __name__ == '__main__':
    main()
