﻿unit KakaoUtils;

interface

uses
  Winapi.Windows, Winapi.Messages, System.SysUtils, System.Variants, System.Classes, Generics.Collections, AOBScanAPI, MemAPI,
  System.Win.ScktComp, SharableMemory, ProcessAPI, IniFiles, vcl.clipbrd, MD5, Vcl.Graphics, FileUtils, System.IOUtils;

const
  IMG_MAX_DOWNLOAD_COUNT = 10;

type
  TState = (None, AddFriend, SearchFriend, ViewFriend, ViewProfileImage, BlockAndClearFriend);

  TKakao = class
    private
      FInstance: DWORD;

      FKakaoHandle: THandle;
      FOnlineMainViewHandle: THandle;
      FContactListHandle: THandle;
      FSearchListHandle: THandle;

      FHookFriendName1: DWORD;
      FHookFriendName2: DWORD;
      FHookSearchCount: DWORD;
      FHookAddFriendSelectCountry: DWORD;
      FHookIsLocal: DWORD;
      FHookSaveFile: DWORD;
      FFuncSaveFile: DWORD;
      FHookSaveFileCustom: DWORD;
      FFuncNextProfile: DWORD;
      FHookHttpRespon: DWORD;
      FHookBlockCount: DWORD;
      FHookSyncFriend: DWORD;
      FHookShowImage: DWORD;

      FSharableMemoryCache: Pointer;

      FState: TState;

      FSharableInstance: PSharableInstance;

      FProcessId: Cardinal;
    public
      property HookFriendName1: DWORD read FHookFriendName1 write FHookFriendName1;
      property HookFriendName2: DWORD read FHookFriendName2 write FHookFriendName2;
      property HookSearchCount: DWORD read FHookSearchCount write FHookSearchCount;
      property HookAddFriendSelectCountry: DWORD read FHookAddFriendSelectCountry write FHookAddFriendSelectCountry;
      property HookIsLocal: DWORD read FHookIsLocal write FHookIsLocal;
      property HookSaveFile: DWORD read FHookSaveFile write FHookSaveFile;
      property FuncSaveFile: DWORD read FFuncSaveFile write FFuncSaveFile;
      property HookSaveFileCustom: DWORD read FHookSaveFileCustom write FHookSaveFileCustom;
      property FuncNextProfile: DWORD read FFuncNextProfile write FFuncNextProfile;
      property HookHttpRespon: DWORD read FHookHttpRespon write FHookHttpRespon;
      property HookBlockCount: DWORD read FHookBlockCount write FHookBlockCount;
      property HookSyncFriend: DWORD read FHookSyncFriend write FHookSyncFriend;
      property HookShowImage: DWORD read FHookShowImage write FHookShowImage;

      property SharableInstance: PSharableInstance read FSharableInstance write FSharableInstance;

      function IsInjectedKakaoTalk: Boolean;
      function GetSharableMemory: Pointer;
      procedure CreateSharable;

      procedure ClickFriendList;
      function AddFriend(PhoneNumber: String): Boolean;
      function SearchFriend(CustomName: String): Boolean;
      function ViewFriend: Boolean;
      function ViewProfileImage(CustomName: String): Boolean;
      function ViewPreviewImage: Boolean;
      function BlockAndClearFriend: Boolean;
      function SyncFriend: Boolean;

      constructor Create;
      destructor Destroy; override;
  end;

var
  CriticalSection: TRTLCriticalSection;

implementation
  uses uKakaoHandle, uHookSignature;

const
  // Thread Wait
  WAIT_TIME = 10 * 1000;

function WaitFor(Thread: TThread; WaitTime: Integer = WAIT_TIME): Boolean;
begin
  Result:= True;

  if WaitForSingleObject(Thread.Handle, WaitTime) = WAIT_TIMEOUT then
  begin
    try
      try
        SuspendThread(Thread.Handle);
        TerminateThread(Thread.Handle, 0);
      finally
        while CriticalSection.LockCount <> -1 do
          LeaveCriticalSection(CriticalSection);

        Result:= False;
      end;
    except;
    end;
  end;

  Thread.Free;
end;

procedure Click(hWindow: THandle; x, y: Integer);
begin
  SendMessage(hWindow, WM_LBUTTONDOWN, MK_LBUTTON, MAKELPARAM(x, y));
  SendMessage(hWindow, WM_LBUTTONUP, MK_LBUTTON, MAKELPARAM(x, y));
end;

procedure RClick(hWindow: THandle; x, y: Integer);
begin
  SendMessage(hWindow, WM_RBUTTONDOWN, MK_RBUTTON, MAKELPARAM(x, y));
  SendMessage(hWindow, WM_RBUTTONUP, MK_RBUTTON, MAKELPARAM(x, y));
end;

constructor TKakao.Create;
var
  Process: TProcess;
  Ini: TIniFile;
begin
  FKakaoHandle:= FindWindow('EVA_Window_Dblclk', '카카오톡');
  FOnlineMainViewHandle:= FindWindowEx(FKakaoHandle, 0, 'EVA_ChildWindow', nil);
  FContactListHandle:= FindWindowEx(FOnlineMainViewHandle, 0, 'EVA_Window', nil);
  FSearchListHandle:= FindWindowEx(FContactListHandle, 0, 'EVA_VH_ListControl_Dblclk', nil);

  FState:= None;

  FInstance:= GetModuleHandle(nil);

  const IniPath = Format('C:\%s.ini', [APP_NAME]);

  if IsInjectedKakaoTalk then
  begin
    DeleteFile(IniPath);

    SetWindowPos(FKakaoHandle, HWND_NOTOPMOST, 0, 0, 0, 0, SWP_NOMOVE);

    ScanStructure.hProcess:= GetCurrentProcess;
    ScanStructure.StartAddr:= FInstance;
    ScanStructure.EndAddr:= FInstance * 2;

    // 시그너쳐 스캔
    FHookFriendName1:= AOBSCAN(SIG_FRIENDNAME1, 0);
    FHookFriendName2:= AOBSCAN(SIG_FRIENDNAME2, 0) + 7;
    FHookSearchCount:= AOBSCAN(SIG_SEARCH_COUNT, 0);
    FHookAddFriendSelectCountry:= AOBSCAN(SIG_ADD_FRIEND_SELECT_COUNTRY, 0);
    FHookIsLocal:= AOBSCAN(SIG_IS_LOCAL_PROFILE, 0) + $13;
    FHookSaveFile:= AOBSCAN(SIG_SAVE_PROFILE, 1);
    FFuncSaveFile:= AOBSCAN(SIG_SAVE_PROFILE_DIALOG, 0);
    FHookSaveFileCustom:= AOBSCAN(SIG_SAVE_PROFILE_DIALOG_CUSTOM, 0) + $A;
//    FFuncNextProfile:= AOBSCAN(SIG_NEXT_PROFILE, 0);
    FHookHttpRespon:= AOBSCAN(SIG_HTTP_RESPON, 1) + $A;
//    FHookBlockCount:= AOBSCAN(SIG_BLOCK_COUNT, 0) + $B;
//    FHookSyncFriend:= AOBSCAN(SIG_SYNC_FRIEND, 0) + $10;
    FHookShowImage:= AOBSCAN(SIG_SHOW_IMAGE, 0);

    // 공유 메모리 할당
    FSharableInstance:= VirtualAlloc(nil, $4000, MEM_COMMIT, PAGE_EXECUTE_READWRITE);

    Ini:= TiniFile.Create(IniPath);
    try
      Ini.WriteString(APP_NAME, 'pSharable', DWORD(FSharableInstance).ToString);
    finally
      Ini.Free;
    end;
  end
  else begin
    if Process.GetProcessId('KakaoTalk.exe') then
    begin
      FProcessId:= Process.Id;
    end;
  end;
end;

procedure TKakao.CreateSharable;
begin
  if not Assigned(SharableMemory.PSharable) then
    SharableMemory.SharableInstance.Init(OpenProcess(PROCESS_ALL_ACCESS, False, FProcessId), GetSharableMemory);
end;

destructor TKakao.Destroy;
begin
  inherited;
end;

function TKakao.IsInjectedKakaoTalk: Boolean;
begin
  Result:= FInstance = GetModuleHandle('KakaoTalk.exe');
end;

function TKakao.GetSharableMemory: Pointer;
var
  Ini: TIniFile;
begin
  if Assigned(FSharableMemoryCache) then
    Exit(FSharableMemoryCache);

  const IniPath = Format('C:\%s.ini', [APP_NAME]);
  Ini:= TiniFile.Create(IniPath);
  try
    FSharableMemoryCache:= Ptr(Ini.ReadString(APP_NAME, 'pSharable', '0').ToInteger);
  finally
    Ini.Free;
  end;

  Result:= FSharableMemoryCache;
end;

procedure TKakao.ClickFriendList;
begin
  // 친구목록 아이콘 클릭
  Click(FOnlineMainViewHandle, 30, 30);
end;

function TKakao.AddFriend(PhoneNumber: String): Boolean;
var
  Thread: TThread;
  hSelectCountry: THandle;
begin
  Thread:= TThread.CreateAnonymousThread(procedure
  begin
    CreateSharable;
    SharableMemory.SharableInstance.UpdateMemory;
    SharableMemory.SharableInstance.AddFriendResult:= ADD_NONE;
    ZeroMemory(@SharableMemory.SharableInstance.RequestPhoneNumber, SizeOf(SharableMemory.SharableInstance.RequestPhoneNumber));
    CopyMemory(@SharableMemory.SharableInstance.RequestPhoneNumber, PDWORD(PhoneNumber), Length(PhoneNumber) * 2 + 2);
    SharableMemory.SharableInstance.WriteMemory;

    while True do
    begin
      Sleep(250);

      EnterCriticalSection(CriticalSection);
      try
        FState:= TState.AddFriend;

        ClickFriendList;

        AddFriendHandle:= 0;
        EnumWindows(@IsVisibleAddFriendCallback, FKakaoHandle);
        if AddFriendHandle = 0 then
        begin
          // 친구 추가 버튼 클릭
          Click(FContactListHandle, 290, 30);
        end
        else begin
          // 번호 입력 후 추가
          var AddFriendFrameHandle:= FindWindowEx(AddFriendHandle, 0, '#32770', nil);
          if AddFriendFrameHandle > 0 then
          begin
            Sleep(250);

            AddFriendFrameHandle:= FindWindowEx(AddFriendHandle, AddFriendFrameHandle, '#32770', nil);
            // 국가 선택
            repeat
              Click(AddFriendFrameHandle, 50, 90);
              hSelectCountry:= FindWindowEx(FindWindow('EVA_Menu', nil), 0, 'EVA_Window', nil);

              Sleep(250);
            until (hSelectCountry > 0);
            Click(hSelectCountry, 20, 20);

            const hEditFriendName = FindWindowEx(AddFriendFrameHandle, 0, 'Edit', nil);
            const hEditPhoneNumber = FindWindowEx(AddFriendFrameHandle, hEditFriendName, 'Edit', nil);

            if (IsWindowEnabled(hEditFriendName)) And (IsWindowEnabled(hEditPhoneNumber)) then
            begin
              Sleep(200);

              SendMessage(hEditFriendName, WM_SETTEXT, 0, LParam(PChar(PhoneNumber)));
              SendMessage(hEditPhoneNumber, WM_SETTEXT, 0, LParam(PChar(PhoneNumber)));

              Sleep(200);
              PostMessage(AddFriendFrameHandle, WM_KEYDOWN, VK_RETURN, 0);

              while FindWindowEx(AddFriendHandle, 0, '#32770', nil) > 0 do
              begin
                Sleep(500);
                SendMessage(AddFriendHandle, WM_CLOSE, 0, 0);
              end;

//              SyncFriend;

              Exit;
            end;
          end;
        end;

      finally
        LeaveCriticalSection(CriticalSection);
      end;
    end;
  end);

  Thread.FreeOnTerminate:= False;
  Thread.Start;

  Result:= WaitFor(Thread, 5 * 1000);
end;

function TKakao.SearchFriend(CustomName: String): Boolean;
var
  Thread: TThread;
  p: TPoint;
begin
  Thread:= TThread.CreateAnonymousThread(procedure
  begin
    CreateSharable;
//    SyncFriend;

    SharableMemory.SharableInstance.UpdateMemory;
    SharableMemory.SharableInstance.SearchCount:= -1;
    SharableMemory.SharableInstance.WriteMemory;

//    while True do
//    begin
      EnterCriticalSection(CriticalSection);
      try
        FState:= TState.SearchFriend;

        ClickFriendList;

        const hSearchEdit = FindWindowEx(FContactListHandle, 0, 'Edit', nil);

//        repeat
          // 친구 검색 버튼 클릭
//          p.Create(255, 30);
//          SendMessage(FContactListHandle, WM_LBUTTONDOWN, MK_LBUTTON, MAKELPARAM(p.x, p.y));
//          SendMessage(FContactListHandle, WM_LBUTTONUP, MK_LBUTTON, MAKELPARAM(p.x, p.y));

//          SendMessage(hSearchEdit, WM_SETTEXT, 0, LParam(PChar('')));

//          Sleep(500);
//        until ((IsWindowEnabled(hSearchEdit)) And (GetWindowCaption(hSearchEdit) = ''));

        SendMessage(hSearchEdit, WM_SETTEXT, 0, LParam(PChar(CustomName)));

        Sleep(100);

        Exit;

      finally
        LeaveCriticalSection(CriticalSection);
      end;
//    end;
  end);

  Thread.FreeOnTerminate:= False;
  Thread.Start;

  Result:= WaitFor(Thread, 5 * 1000);
end;

function TKakao.SyncFriend: Boolean;
begin
  FriendConfigHandle:= 0;
  EnumWindows(@GetFriendConfigHandleCallback, 0);
  if FriendConfigHandle > 0 then
  begin
    for var i:= 1 to 3 do
    begin
      Click(GetParent(FriendConfigHandle), 70, 125); // 친구 동기화
      Sleep(Random(100));
    end;
  end;

  Result:= True;
end;

function TKakao.ViewFriend: Boolean;
var
  Thread: TThread;
begin
  Thread:= TThread.CreateAnonymousThread(procedure
  begin
    CreateSharable;
    SharableMemory.SharableInstance.gSaveStep:= 1;

    while True do
    begin
      Sleep(500);

      EnterCriticalSection(CriticalSection);
      try
        FState:= TState.ViewFriend;

        // 프로필 아이콘 클릭
        Click(FSearchListHandle, 40, 50);

        ViewFriendHandle:= 0;
        EnumWindows(@GetViewFriendHandleCallback, FKakaoHandle);

        if ViewFriendHandle > 0 then
        begin
          // edit name -> cancel
          Sleep(100);

          Click(ViewFriendHandle, 210, 444);
          PostMessage(ViewFriendHandle, WM_KEYDOWN, VK_ESCAPE, 0);

          Exit;
        end;

      finally
        LeaveCriticalSection(CriticalSection);
      end;
    end;
  end);

  Thread.FreeOnTerminate:= False;
  Thread.Start;

  Result:= WaitFor(Thread, 3 * 1000);
end;

function SafeImageMenuClick(x, y: Integer): Boolean;
var
  BeforeTick: DWORD;
  BeforeImageShowCount: DWORD;
begin
  SharableMemory.SharableInstance.UpdateMemory;
  BeforeImageShowCount:= SharableMemory.SharableInstance.pImageShowCount;

  Click(ViewFriendHandle, x, y);

  BeforeTick:= GetTickCount + 1000;
  while BeforeTick > GetTickCount do
  begin
    Sleep(50);

    SharableMemory.SharableInstance.UpdateMemory;
    if SharableMemory.SharableInstance.pImageShowCount <> BeforeImageShowCount then
      Exit(True);
  end;

  Exit(False);
end;

function TKakao.ViewProfileImage(CustomName: String): Boolean;
var
  Thread: TThread;
  Source: String;
  GetViewProfileHandleCount: Integer;
  ImagePage: TArray<String>;
  Rect: TRect;
  BeforeImagePage: String;
  FileCase: Array [0..1] of String;
  ViewFriendBitmap: TBitmap;
  BaseDir: WideString;
begin
  Thread:= TThread.CreateAnonymousThread(procedure
  begin
    CreateSharable;

    // 유저 데이터 폴더 생성
    BaseDir:= ROOT + StrToMD5(AnsiString(CustomName));
    if DirectoryExists(BaseDir) then
    begin
      TDirectory.Delete(BaseDir, True);
    end;
    CreateDir(BaseDir);
    CreateDir(Format('%s\%s', [BaseDir, IIS_PROFILE_PATH]));
    CreateDir(Format('%s\%s', [BaseDir, IIS_BG_PATH]));
    CreateDir(Format('%s\%s', [BaseDir, IIS_PREVIEW_PATH]));

    GetViewProfileHandleCount:= 0;

    var Step:= 1;

    while True do
    begin
      try
        Sleep(100);

        EnterCriticalSection(CriticalSection);
        try
          FState:= TState.ViewProfileImage;

          // 프로필 이미지 없음
          if GetViewProfileHandleCount >= 2 then
          begin
            Inc(Step);

            if Step > 2 then
              Exit;
          end;


          var tStep:= 1;
          var IsSuccess:= False;
          if Step = 1 then
          begin
            SharableMemory.SharableInstance.UpdateMemory;
            SharableMemory.SharableInstance.gSaveStep:= 1;
            SharableMemory.SharableInstance.WriteMemory;
            IsSuccess:= SafeImageMenuClick(150, 390);
          end else
          begin
            tStep:= 2;
            SharableMemory.SharableInstance.UpdateMemory;
            SharableMemory.SharableInstance.gSaveStep:= 2;
            SharableMemory.SharableInstance.WriteMemory;
            IsSuccess:= SafeImageMenuClick(20, 20);
          end;

          repeat
            Sleep(10);
            SharableMemory.SharableInstance.UpdateMemory;
          until (SharableMemory.SharableInstance.gSaveStep = tStep);

          if not IsSuccess then
          begin
            Inc(GetViewProfileHandleCount);
            Continue;
          end;

          // 프로필 이미지 뷰어 핸들을 구한다
          Source:= CustomName + ' 1/';
          ViewProfileHandle:= 0;
          EnumWindows(@GetViewProfileHandleCallback, DWORD(Source));

          if ViewProfileHandle > 0 then
          begin

            SetWindowPos(ViewProfileHandle, HWND_TOPMOST, 0, 0, 0, 0, SWP_NOMOVE);
            Sleep(250);

            GetWindowRect(ViewProfileHandle, Rect);
  //          writeln(Rect.Width, ' / ' , Rect.Height);
//            if (Rect.Width = 565) And (Rect.Height = 470) then
            begin
              BeforeImagePage:= '0';

              while True do
              begin
                Sleep(10);

                ShowWindowAsync(ViewProfileHandle, SW_SHOWNORMAL);
                SetForegroundWindow(ViewProfileHandle);

                // 파일 저장 실패 메세지가 있다면 닫아줌
                EnumWindows(@CloseProfileSaveFailCallback, FKakaoHandle);

                ImagePage:= GetWindowCaption(ViewProfileHandle).Split([' ', '/']);

                const CurrentPage = ImagePage[1].ToInteger;
                // max save image count, check end of image
                if (CurrentPage >= IMG_MAX_DOWNLOAD_COUNT) Or ((Length(ImagePage) = 3) And (ImagePage[1].Equals(ImagePage[2]))) then
                begin
                  Sleep(250);

                  Inc(Step);

                  if Step > 2 then
                    Exit;

                  break;
                end;

                if BeforeImagePage.Equals(ImagePage[1]) then
                begin
                  // set next file
                  SetCursorPos(Rect.Left + 530, Rect.Top + 230);
                  Click(ViewProfileHandle, 530, 230);

                  Continue;
                end;

                // check file download
                var Path:= IIS_PROFILE_PATH;
                if Step > 1 then
                  Path:= IIS_BG_PATH;

                FileCase[0]:= Format('%s%s\%s\%s.mp4', [ROOT, StrToMD5(AnsiString(SharableMemory.SharableInstance.GetFriendCustomName)), Path, ImagePage[1]]);
                FileCase[1]:= Format('%s.jpg', [FileCase[0]]);

                const TimeOut = GetTickCount + 1500;
                var IsExists:= False;
                while GetTickCount < TimeOut do
                begin
                  Sleep(10);

                  if CurrentPage = 1 then
                  begin
                    if ((Step >= 2) And (SharableMemory.SharableInstance.IsEmptyBackgroundImage)) Or
                       ((Step = 1) And (SharableMemory.SharableInstance.IsEmptyProfileImage)) then
                    begin
                      break;
                    end;
                  end;

                  for var f in FileCase do
                  begin
                    if (FileExists(f)) And (FileSize(f) > 2100) then
                    begin
                      IsExists:= True;
                      break;
                    end;
                  end;

                  if IsExists then
                    break;
                end;

                BeforeImagePage:= ImagePage[1];

              end;
            end;
          end
          else begin
//            if Step = 1 then
//              Sleep(1000);

            Inc(GetViewProfileHandleCount);
          end;

        finally
          LeaveCriticalSection(CriticalSection);
        end;
      except;
        Exit;
      end;
    end;
  end);

  Thread.FreeOnTerminate:= False;
  Thread.Start;

  Result:= WaitFor(Thread, 40 * 1000);

end;

function TKakao.ViewPreviewImage: Boolean;
type TFirstImage = record
  Path: String;
  Point: TPoint;
end;
var
  Thread: TThread;
  Source: String;
  ViewFriendBitmap: TBitmap;
  FirstImage: Array [0..1] of TFirstImage;
begin
  Thread:= TThread.CreateAnonymousThread(procedure
  begin
    CreateSharable;
    SharableMemory.SharableInstance.UpdateMemory;
    SharableMemory.SharableInstance.gSaveStep:= 1;
    SharableMemory.SharableInstance.WriteMemory;
    while True do
    begin
      SharableMemory.SharableInstance.UpdateMemory;
      if SharableMemory.SharableInstance.gSaveStep = 1 then
        break;
    end;

    EnterCriticalSection(CriticalSection);
    try
      FState:= TState.ViewProfileImage;

      const FriendCustomName = StrToMD5(AnsiString(SharableMemory.SharableInstance.GetFriendCustomName));

      ViewFriendBitmap:= GetProfileScreen(ViewFriendHandle);
      try
        ViewFriendBitmap.SaveToFile(Format('%s%s\%s\preview.jpg', [ROOT, FriendCustomName, IIS_PREVIEW_PATH]));
      finally
        ViewFriendBitmap.Free;
      end;

      FirstImage[0].Path:= Format('%s%s\%s\1.mp4.jpg', [ROOT, FriendCustomName, IIS_PROFILE_PATH]);
      FirstImage[0].Point:= TPoint.Create(150, 390);
      FirstImage[1].Path:= Format('%s%s\%s\1.mp4.jpg', [ROOT, FriendCustomName, IIS_BG_PATH]);
      FirstImage[1].Point:= TPoint.Create(20, 20);

      for var i:= 0 to 1 do
      begin
        SharableMemory.SharableInstance.UpdateMemory;
        SharableMemory.SharableInstance.gSaveStep:= i + 1;
        SharableMemory.SharableInstance.WriteMemory;

        repeat
          Sleep(10);
          SharableMemory.SharableInstance.UpdateMemory;
        until (SharableMemory.SharableInstance.gSaveStep = i + 1);

        if (not FileExists(FirstImage[i].Path)) And (SafeImageMenuClick(FirstImage[i].Point.x, FirstImage[i].Point.y)) then
        begin

          const BeforeTick = GetTickCount + 500;
          while GetTickCount < BeforeTick do
          begin
            Sleep(10);
            if (FileExists(FirstImage[i].Path)) And (FileSize(FirstImage[i].Path) > 2100) then
            begin
              break;
            end;
          end;

          Source:= SharableMemory.SharableInstance.GetFriendCustomName;
          ViewProfileHandle:= 0;
          EnumWindows(@GetViewProfileHandleCallback, DWORD(Source));
        end;

        if ViewProfileHandle > 0 then
          SendMessage(ViewProfileHandle, WM_CLOSE, 0, 0);
      end;

    finally
      LeaveCriticalSection(CriticalSection);
    end;

  end);

  Thread.FreeOnTerminate:= False;
  Thread.Start;

  Result:= WaitFor(Thread, 5 * 1000);
end;

function TKakao.BlockAndClearFriend: Boolean;
var
  Thread: TThread;
begin
  Thread:= TThread.CreateAnonymousThread(procedure
  begin
    while True do
    begin
      EnterCriticalSection(CriticalSection);
      try
        FState:= TState.BlockAndClearFriend;

        CreateSharable;
        if SharableMemory.SharableInstance.GetSearchCount = 0 then
          Exit;

        repeat
          Sleep(250);

          // 옵션
          RClick(FSearchListHandle, 100, 50);

          const hFriendConfig = FindWindow('EVA_Menu', nil);
          if hFriendConfig > 0 then
          begin
            Click(hFriendConfig, 50, 230);
            Sleep(250);
          end;

          FriendBlockYnHandle:= 0;
          EnumWindows(@GetFriendBlockYnHandleCallback, FKakaoHandle);
        until (FriendBlockYnHandle > 0);

        // 차단 확인
        while True do
        begin
          FriendBlockYnHandle:= 0;
          EnumWindows(@GetFriendBlockYnHandleCallback, FKakaoHandle);

          if FriendBlockYnHandle > 0 then
            PostMessage(FriendBlockYnHandle, WM_KEYDOWN, VK_RETURN, 0);

          if FriendBlockYnHandle = 0 then
            break;
        end;

        Sleep(1000);

        // 설정 -> 리스트박스에 차단된 친구가 존재하면 해제 후 삭제
        CreateSharable;
        while SharableMemory.SharableInstance.GetBlockCount > 0 do
        begin
          FriendConfigHandle:= 0;
          EnumWindows(@GetFriendConfigHandleCallback, 0);
          if FriendConfigHandle = 0 then
            Exit;

          Click(FriendConfigHandle, 320, 55); // config btn
          Sleep(250);

          if FriendConfigHandle = 0 then
            Continue;

          while True do
          begin
            FriendBlockConfigHandle:= 0;
            EnumWindows(@GetFriendBlockConfigHandleCallback, 0);

            if FriendBlockConfigHandle > 0 then
            begin
              Click(FriendBlockConfigHandle, 70, 145); // select radio
              Sleep(100);
              Click(FriendBlockConfigHandle, 60, 200);
              Sleep(250);
            end;

            if FriendBlockConfigHandle = 0 then
              break;
          end;

          // 다시 친구추가 질문 팝업 닫음
          while True do
          begin
            FriendBlockSuccessHandle:= 0;
            EnumWindows(@GetFriendBlockSuccessHandleCallback, 0);

            if FriendBlockSuccessHandle > 0 then
            begin
              Click(FriendBlockSuccessHandle, 180, 105);
              Sleep(250);
            end;

            if FriendBlockSuccessHandle = 0 then
              break;
          end;

        end;

//        SyncFriend;

        Exit;

      finally
        LeaveCriticalSection(CriticalSection);
      end;
    end;
  end);

  Thread.FreeOnTerminate:= False;
  Thread.Start;

  Result:= WaitFor(Thread, 10 * 1000);
end;


initialization
  InitializeCriticalSection(CriticalSection);

finalization
  DeleteCriticalSection(CriticalSection);

end.
